package com.finova.scheduling;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.account.Account;
import com.finova.account.AccountRepository;
import com.finova.transaction.Transaction;
import com.finova.transaction.TransactionRepository;

/**
 * Builds a monthly statement per account for the previous calendar month.
 *
 * <p>Opening balance is inferred as {@code closing − credits + debits} from transactions in the
 * period (using the current balance as the period's closing balance). Re-runs for the same
 * account+month are skipped so the job is idempotent.
 */
@Service
public class StatementGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountStatementRepository statementRepository;

    public StatementGenerationService(AccountRepository accountRepository,
                                      TransactionRepository transactionRepository,
                                      AccountStatementRepository statementRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
    }

    @Transactional
    public int generateForPreviousMonth() {
        YearMonth period = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        return generateFor(period);
    }

    @Transactional
    public int generateFor(YearMonth period) {
        Instant start = period.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = period.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Account> accounts = accountRepository.findAll();
        int created = 0;

        for (Account account : accounts) {
            if (statementRepository.existsByAccountIdAndPeriodYearAndPeriodMonth(
                    account.getId(), period.getYear(), period.getMonthValue())) {
                continue;
            }

            List<Transaction> txs = transactionRepository
                    .findByAccountIdAndCreatedAtBetween(account.getId(), start, end);

            BigDecimal credits = BigDecimal.ZERO;
            BigDecimal debits = BigDecimal.ZERO;
            for (Transaction tx : txs) {
                if (tx.getTargetAccount() != null && account.getId().equals(tx.getTargetAccount().getId())) {
                    credits = credits.add(tx.getAmount());
                }
                if (tx.getSourceAccount() != null && account.getId().equals(tx.getSourceAccount().getId())) {
                    debits = debits.add(tx.getAmount());
                }
            }

            BigDecimal closing = account.getBalance();
            BigDecimal opening = closing.subtract(credits).add(debits);

            AccountStatement statement = new AccountStatement();
            statement.setAccount(account);
            statement.setPeriodYear(period.getYear());
            statement.setPeriodMonth(period.getMonthValue());
            statement.setOpeningBalance(opening);
            statement.setClosingBalance(closing);
            statement.setTotalCredits(credits);
            statement.setTotalDebits(debits);
            statement.setTransactionCount(txs.size());
            statementRepository.save(statement);
            created++;
        }

        log.info("Generated {} statement(s) for period {}-{}", created, period.getYear(), period.getMonthValue());
        return created;
    }
}
