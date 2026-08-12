package com.finova.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.account.Account;
import com.finova.account.AccountRepository;
import com.finova.account.AccountStatus;
import com.finova.audit.Auditable;
import com.finova.common.config.CacheConfig;
import com.finova.common.exception.BusinessRuleException;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.fraud.FraudDetectionService;
import com.finova.integration.ledger.BlockchainLedgerService;
import com.finova.transaction.dto.MoneyRequest;
import com.finova.transaction.dto.TransactionResponse;
import com.finova.transaction.dto.TransferRequest;
import com.finova.user.User;
import com.finova.user.UserRepository;

/**
 * Money movement: deposits, withdrawals, and transfers, plus statement history.
 *
 * <p>Correctness guarantees:
 * <ul>
 *   <li><b>Atomicity</b> - each operation runs in a single {@code @Transactional} unit, so a debit
 *       and its matching credit either both commit or both roll back; a partial transfer is
 *       impossible.</li>
 *   <li><b>Isolation under concurrency</b> - affected accounts are loaded with a pessimistic write
 *       lock ({@code SELECT ... FOR UPDATE}) acquired in a fixed order (ascending id) to prevent
 *       deadlocks and lost updates when two transfers touch the same accounts at once.</li>
 *   <li><b>Precision</b> - all arithmetic is on {@link BigDecimal} at scale 2 (HALF_EVEN); no
 *       floating point is ever involved in money maths.</li>
 * </ul>
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final CacheManager cacheManager;
    private final FraudDetectionService fraudDetectionService;
    private final BlockchainLedgerService blockchainLedgerService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              TransactionReferenceGenerator referenceGenerator,
                              CacheManager cacheManager,
                              FraudDetectionService fraudDetectionService,
                              BlockchainLedgerService blockchainLedgerService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.referenceGenerator = referenceGenerator;
        this.cacheManager = cacheManager;
        this.fraudDetectionService = fraudDetectionService;
        this.blockchainLedgerService = blockchainLedgerService;
    }

    @Auditable(action = "TRANSFER", targetType = "Account")
    @Transactional
    public TransactionResponse transfer(String username, TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BusinessRuleException("Source and destination accounts must differ");
        }
        BigDecimal amount = normalize(request.amount());
        Long callerId = requireUserId(username);

        // Resolve internal ids only (scalar projection) so no stale versioned entity is cached before
        // we take the row lock; then lock in a stable order (ascending id) to avoid deadlocks.
        Long fromId = accountRepository.findIdByPublicId(request.fromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Long toId = accountRepository.findIdByPublicId(request.toAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        LockedPair locked = lockInOrder(fromId, toId);
        Account from = locked.byId(fromId);
        Account to = locked.byId(toId);

        // Only the owner may debit their own account (checked on the locked, authoritative row).
        if (!from.getOwner().getId().equals(callerId)) {
            throw new ResourceNotFoundException("Source account not found");
        }

        ensureActive(from, "Source");
        ensureActive(to, "Destination");
        ensureSufficientFunds(from, amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        Transaction tx = record(TransactionType.TRANSFER, from, to, amount, request.description());
        evictBalance(from);
        evictBalance(to);

        // Advisory fraud screening on the debited account; raises flags but never blocks the transfer.
        fraudDetectionService.evaluateDebit(from, tx);

        log.info("Transfer {} of {} from {} to {}", tx.getReference(), amount,
                from.getPublicId(), to.getPublicId());
        return TransactionResponse.from(tx);
    }

    @Auditable(action = "DEPOSIT", targetType = "Account")
    @Transactional
    public TransactionResponse deposit(String username, UUID accountId, MoneyRequest request) {
        BigDecimal amount = normalize(request.amount());
        Account account = lockOwnedAccount(username, accountId);
        ensureActive(account, "Account");

        account.setBalance(account.getBalance().add(amount));
        Transaction tx = record(TransactionType.DEPOSIT, null, account, amount, request.description());
        evictBalance(account);

        log.info("Deposit {} of {} into {}", tx.getReference(), amount, account.getPublicId());
        return TransactionResponse.from(tx);
    }

    @Auditable(action = "WITHDRAWAL", targetType = "Account")
    @Transactional
    public TransactionResponse withdraw(String username, UUID accountId, MoneyRequest request) {
        BigDecimal amount = normalize(request.amount());
        Account account = lockOwnedAccount(username, accountId);
        ensureActive(account, "Account");
        ensureSufficientFunds(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        Transaction tx = record(TransactionType.WITHDRAWAL, account, null, amount, request.description());
        evictBalance(account);
        fraudDetectionService.evaluateDebit(account, tx);

        log.info("Withdrawal {} of {} from {}", tx.getReference(), amount, account.getPublicId());
        return TransactionResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(String username, UUID accountId, Pageable pageable) {
        Long ownerId = requireUserId(username);
        Account account = accountRepository.findByPublicIdAndOwnerId(accountId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return transactionRepository.findForAccount(account.getId(), pageable)
                .map(TransactionResponse::from);
    }

    // --- helpers ---

    private Transaction record(TransactionType type, Account source, Account target,
                               BigDecimal amount, String description) {
        Transaction tx = new Transaction();
        tx.setReference(referenceGenerator.generate());
        tx.setType(type);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setSourceAccount(source);
        tx.setTargetAccount(target);
        tx.setAmount(amount);
        tx.setCurrency("INR");
        tx.setDescription(description);
        Transaction saved = transactionRepository.save(tx);
        // Conceptual immutable mirror of the ledger entry (sandbox blockchain simulation).
        blockchainLedgerService.append(saved.getReference(),
                type.name() + ":" + amount + ":INR");
        return saved;
    }

    private Account lockOwnedAccount(String username, UUID accountId) {
        Long ownerId = requireUserId(username);
        // Resolve id via projection (no pre-load), lock the row, then verify ownership on the locked row.
        Long accountInternalId = accountRepository.findIdByPublicId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Account account = accountRepository.findByIdForUpdate(accountInternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!account.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Account not found");
        }
        return account;
    }

    /** Locks up to two accounts in ascending id order so concurrent transfers cannot deadlock. */
    private LockedPair lockInOrder(Long idA, Long idB) {
        List<Long> ordered = java.util.stream.Stream.of(idA, idB)
                .sorted(Comparator.naturalOrder())
                .toList();
        Account first = accountRepository.findByIdForUpdate(ordered.get(0))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Account second = accountRepository.findByIdForUpdate(ordered.get(1))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return new LockedPair(first, second);
    }

    private void ensureActive(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(label + " account is " + account.getStatus().name().toLowerCase()
                    + " and cannot be used");
        }
    }

    private void ensureSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds");
        }
    }

    private Long requireUserId(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void evictBalance(Account account) {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_ACCOUNT_BALANCE);
        if (cache != null) {
            // Key must match AccountService.getBalance: "<ownerUsername>:<accountPublicId>".
            cache.evict(account.getOwner().getUsername() + ":" + account.getPublicId());
        }
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Small holder so callers can fetch either locked account by its id without re-querying. */
    private record LockedPair(Account first, Account second) {
        Account byId(Long id) {
            return first.getId().equals(id) ? first : second;
        }
    }
}
