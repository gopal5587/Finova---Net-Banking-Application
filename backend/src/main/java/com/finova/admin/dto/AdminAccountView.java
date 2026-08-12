package com.finova.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.finova.account.Account;

/**
 * Admin-facing account view. Unlike the customer view it includes the owner's username (for
 * oversight) but still masks the account number to keep full numbers out of admin screens/logs.
 */
public record AdminAccountView(
        UUID id,
        String ownerUsername,
        String maskedAccountNumber,
        String accountType,
        String currency,
        BigDecimal balance,
        String status,
        Instant createdAt
) {
    public static AdminAccountView from(Account account) {
        String number = account.getAccountNumber();
        String masked = (number != null && number.length() > 4)
                ? "****" + number.substring(number.length() - 4)
                : "****";
        return new AdminAccountView(
                account.getPublicId(),
                account.getOwner().getUsername(),
                masked,
                account.getAccountType().name(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
