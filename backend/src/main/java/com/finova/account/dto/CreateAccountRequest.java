package com.finova.account.dto;

import java.math.BigDecimal;

import com.finova.account.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to open a new account.
 *
 * @param accountType     SAVINGS or CURRENT (required)
 * @param initialDeposit  optional opening balance; must be >= 0 with at most 2 decimals
 * @param pan             optional Indian PAN (KYC); stored encrypted at rest
 */
public record CreateAccountRequest(

        @NotNull
        AccountType accountType,

        @DecimalMin(value = "0.00", message = "must not be negative")
        @Digits(integer = 17, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal initialDeposit,

        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "must be a valid PAN (e.g. ABCDE1234F)")
        String pan
) {
}
