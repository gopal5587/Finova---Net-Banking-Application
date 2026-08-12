package com.finova.scheduling;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {

    boolean existsByAccountIdAndPeriodYearAndPeriodMonth(Long accountId, int periodYear, int periodMonth);

    Optional<AccountStatement> findByAccountIdAndPeriodYearAndPeriodMonth(Long accountId, int year, int month);
}
