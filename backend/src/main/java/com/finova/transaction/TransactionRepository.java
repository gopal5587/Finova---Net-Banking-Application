package com.finova.transaction;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Counts recent debits from an account, used by the velocity fraud rule. */
    long countBySourceAccountIdAndCreatedAtAfter(Long sourceAccountId, Instant after);

    /** All ledger rows touching an account inside a closed-open time window [start, end). */
    @Query("""
            select t from Transaction t
            where (t.sourceAccount.id = :accountId or t.targetAccount.id = :accountId)
              and t.createdAt >= :start and t.createdAt < :end
            order by t.createdAt asc
            """)
    List<Transaction> findByAccountIdAndCreatedAtBetween(@Param("accountId") Long accountId,
                                                         @Param("start") Instant start,
                                                         @Param("end") Instant end);

    /**
     * Statement view for a single account: every entry where the account is either the source or
     * the target, newest first. Pageable keeps large histories from being loaded all at once.
     */
    @Query("""
            select t from Transaction t
            where t.sourceAccount.id = :accountId or t.targetAccount.id = :accountId
            order by t.createdAt desc
            """)
    Page<Transaction> findForAccount(@Param("accountId") Long accountId, Pageable pageable);
}
