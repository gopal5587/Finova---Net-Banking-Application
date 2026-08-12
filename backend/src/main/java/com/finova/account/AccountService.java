package com.finova.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.account.dto.AccountResponse;
import com.finova.account.dto.BalanceResponse;
import com.finova.account.dto.CreateAccountRequest;
import com.finova.audit.Auditable;
import com.finova.common.config.CacheConfig;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.user.User;
import com.finova.user.UserRepository;

/**
 * Account lifecycle and balance reads.
 *
 * <p>Ownership is enforced on every operation by scoping queries to the caller's user id, so one
 * customer can never see or touch another's account. Balances are read through a Redis cache
 * (keyed per owner+account) and evicted whenever the account is created or mutated.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Auditable(action = "ACCOUNT_CREATE", targetType = "Account")
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.CACHE_ACCOUNT_BALANCE, key = "#username + ':' + #result.id()")
    public AccountResponse createAccount(String username, CreateAccountRequest request) {
        User owner = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = new Account();
        account.setOwner(owner);
        account.setAccountNumber(uniqueAccountNumber());
        account.setAccountType(request.accountType());
        account.setCurrency("INR");
        account.setStatus(AccountStatus.ACTIVE);
        account.setPan(request.pan());

        BigDecimal opening = normalize(request.initialDeposit());
        account.setBalance(opening);

        Account saved = accountRepository.save(account);
        log.info("Opened {} account {} for user '{}' with opening balance {}",
                saved.getAccountType(), saved.getPublicId(), username, opening);
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(String username) {
        Long ownerId = ownerId(username);
        return accountRepository.findByOwnerId(ownerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String username, UUID accountPublicId) {
        return AccountResponse.from(requireOwnedAccount(username, accountPublicId));
    }

    /**
     * Returns the account balance, served from Redis when warm. The key is scoped to the owner so a
     * cache hit can only ever be produced by the account's owner (who proved ownership on the miss).
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.CACHE_ACCOUNT_BALANCE, key = "#username + ':' + #accountPublicId")
    public BalanceResponse getBalance(String username, UUID accountPublicId) {
        Account account = requireOwnedAccount(username, accountPublicId);
        log.debug("Balance cache miss for account {} (owner '{}') - read from DB", accountPublicId, username);
        return new BalanceResponse(account.getPublicId(), account.getCurrency(), account.getBalance());
    }

    private Account requireOwnedAccount(String username, UUID accountPublicId) {
        Long ownerId = ownerId(username);
        return accountRepository.findByPublicIdAndOwnerId(accountPublicId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private Long ownerId(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    private String uniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            String candidate = accountNumberGenerator.generate();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        // Collisions are near-impossible; exhausting retries signals a deeper problem worth surfacing.
        throw new IllegalStateException("Unable to allocate a unique account number");
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
