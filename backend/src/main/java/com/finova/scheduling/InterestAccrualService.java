package com.finova.scheduling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.account.Account;
import com.finova.account.AccountRepository;
import com.finova.account.AccountStatus;
import com.finova.account.AccountType;
import com.finova.common.config.CacheConfig;
import com.finova.transaction.Transaction;
import com.finova.transaction.TransactionReferenceGenerator;
import com.finova.transaction.TransactionRepository;
import com.finova.transaction.TransactionStatus;
import com.finova.transaction.TransactionType;

/**
 * Credits monthly interest to active savings accounts.
 *
 * <p>Formula: {@code interest = balance × (annualRate / 12)}, rounded HALF_EVEN to 2 decimals.
 * Each credit is written as an {@link TransactionType#INTEREST} ledger row so the statement
 * history stays complete. Accounts are locked one-by-one to stay consistent under concurrency.
 */
@Service
public class InterestAccrualService {

    private static final Logger log = LoggerFactory.getLogger(InterestAccrualService.class);
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final CacheManager cacheManager;
    private final BigDecimal annualRate;

    public InterestAccrualService(AccountRepository accountRepository,
                                  TransactionRepository transactionRepository,
                                  TransactionReferenceGenerator referenceGenerator,
                                  CacheManager cacheManager,
                                  @Value("${finova.scheduling.savings-annual-rate:0.04}") BigDecimal annualRate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.referenceGenerator = referenceGenerator;
        this.cacheManager = cacheManager;
        this.annualRate = annualRate;
    }

    @Transactional
    public int accrueMonthlyInterest() {
        List<Account> candidates = accountRepository
                .findByAccountTypeAndStatus(AccountType.SAVINGS, AccountStatus.ACTIVE);

        int credited = 0;
        for (Account ref : candidates) {
            Account account = accountRepository.findByIdForUpdate(ref.getId()).orElse(null);
            if (account == null || account.getBalance().signum() <= 0) {
                continue;
            }

            BigDecimal interest = account.getBalance()
                    .multiply(annualRate)
                    .divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_EVEN);

            if (interest.signum() <= 0) {
                continue;
            }

            account.setBalance(account.getBalance().add(interest));
            recordInterest(account, interest);
            evictBalance(account);
            credited++;
            log.info("Credited interest {} to savings account {}", interest, account.getPublicId());
        }

        log.info("Interest accrual finished: {} account(s) credited at annual rate {}", credited, annualRate);
        return credited;
    }

    private void recordInterest(Account account, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setReference(referenceGenerator.generate());
        tx.setType(TransactionType.INTEREST);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setTargetAccount(account);
        tx.setAmount(amount);
        tx.setCurrency(account.getCurrency());
        tx.setDescription("Monthly savings interest");
        transactionRepository.save(tx);
    }

    private void evictBalance(Account account) {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_ACCOUNT_BALANCE);
        if (cache != null) {
            cache.evict(account.getOwner().getUsername() + ":" + account.getPublicId());
        }
    }
}
