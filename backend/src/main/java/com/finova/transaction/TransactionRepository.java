package com.finova.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

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
