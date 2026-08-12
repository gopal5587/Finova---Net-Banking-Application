package com.finova.fraud;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.finova.account.Account;
import com.finova.transaction.Transaction;
import com.finova.transaction.TransactionRepository;

/**
 * Evaluates fraud rules against a just-completed transfer and records any {@link FraudFlag}s.
 *
 * <p>Detection is advisory: it never blocks or reverses the transfer (that is an admin decision),
 * it only surfaces suspicious activity for review. Runs in the caller's transaction so a flag is
 * committed atomically with the transfer that triggered it.
 *
 * <p>Rules:
 * <ul>
 *   <li><b>LARGE_AMOUNT</b> - amount at/above a configured threshold.</li>
 *   <li><b>VELOCITY</b> - too many debits from the same account inside a short window.</li>
 *   <li><b>ODD_HOURS</b> - activity during the configured overnight window (UTC).</li>
 * </ul>
 */
@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudFlagRepository fraudFlagRepository;
    private final TransactionRepository transactionRepository;
    private final FraudProperties properties;

    public FraudDetectionService(FraudFlagRepository fraudFlagRepository,
                                 TransactionRepository transactionRepository,
                                 FraudProperties properties) {
        this.fraudFlagRepository = fraudFlagRepository;
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    /**
     * Assesses a completed transfer and persists any flags raised.
     *
     * @param sourceAccount the debited account (guaranteed non-null for transfers/withdrawals)
     * @param transaction   the recorded transaction
     */
    public void evaluateDebit(Account sourceAccount, Transaction transaction) {
        if (sourceAccount == null || transaction == null) {
            return; // Nothing to assess (e.g. a pure deposit has no source account).
        }

        List<FraudFlag> flags = new ArrayList<>();

        if (transaction.getAmount().compareTo(properties.largeAmountThreshold()) >= 0) {
            flags.add(build(sourceAccount, transaction, FraudReason.LARGE_AMOUNT, "HIGH",
                    "Amount " + transaction.getAmount() + " >= threshold " + properties.largeAmountThreshold()));
        }

        Instant windowStart = Instant.now().minusSeconds(properties.velocityWindowSeconds());
        long recentDebits = transactionRepository
                .countBySourceAccountIdAndCreatedAtAfter(sourceAccount.getId(), windowStart);
        if (recentDebits > properties.velocityMaxTransfers()) {
            flags.add(build(sourceAccount, transaction, FraudReason.VELOCITY, "MEDIUM",
                    recentDebits + " debits within " + properties.velocityWindowSeconds() + "s"));
        }

        int hourUtc = transaction.getCreatedAt().atZone(ZoneOffset.UTC).getHour();
        if (isOddHour(hourUtc)) {
            flags.add(build(sourceAccount, transaction, FraudReason.ODD_HOURS, "LOW",
                    "Activity at " + hourUtc + ":00 UTC"));
        }

        if (!flags.isEmpty()) {
            fraudFlagRepository.saveAll(flags);
            log.warn("Raised {} fraud flag(s) for account {} on transaction {}",
                    flags.size(), sourceAccount.getPublicId(), transaction.getReference());
        }
    }

    private boolean isOddHour(int hourUtc) {
        return hourUtc >= properties.oddHoursStart() && hourUtc < properties.oddHoursEnd();
    }

    private FraudFlag build(Account account, Transaction tx, FraudReason reason, String severity, String details) {
        FraudFlag flag = new FraudFlag();
        flag.setAccount(account);
        flag.setTransaction(tx);
        flag.setReason(reason);
        flag.setSeverity(severity);
        flag.setDetails(details);
        return flag;
    }
}
