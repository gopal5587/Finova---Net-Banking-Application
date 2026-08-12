package com.finova.scheduling;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.finova.account.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_statements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "period_year", "period_month"}))
@Getter
@Setter
@NoArgsConstructor
public class AccountStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Column(name = "period_year", nullable = false, updatable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false, updatable = false)
    private int periodMonth;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal closingBalance;

    @Column(name = "total_credits", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "total_debits", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false, updatable = false)
    private int transactionCount;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @PrePersist
    void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }
}
