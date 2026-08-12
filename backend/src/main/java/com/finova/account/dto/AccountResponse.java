package com.finova.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.finova.account.Account;

/**
 * Account view returned to clients. The account number is masked (only the last 4 digits shown)
 * to avoid exposing the full number in logs, screenshots, or API responses.
 */
public record AccountResponse(
        UUID id,
        String maskedAccountNumber,
        String accountType,
        String currency,
        BigDecimal balance,
        String status,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getPublicId(),
                mask(account.getAccountNumber()),
                account.getAccountType().name(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "****" + last4;
    }
}
