package com.finova.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.finova.account.Account;
import com.finova.transaction.Transaction;

/**
 * Statement-friendly view of a transaction. Account references are exposed as their public UUIDs
 * only; internal ids are never leaked.
 */
public record TransactionResponse(
        UUID id,
        String reference,
        String type,
        String status,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String description,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getPublicId(),
                tx.getReference(),
                tx.getType().name(),
                tx.getStatus().name(),
                publicId(tx.getSourceAccount()),
                publicId(tx.getTargetAccount()),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }

    private static UUID publicId(Account account) {
        return account == null ? null : account.getPublicId();
    }
}
