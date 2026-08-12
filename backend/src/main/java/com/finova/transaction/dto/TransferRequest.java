package com.finova.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Move money from one account to another.
 *
 * @param fromAccountId source account (must belong to the caller)
 * @param toAccountId   destination account (any account, including the caller's)
 * @param amount        strictly positive, at most 2 decimal places
 * @param description   optional free-text note shown on the statement
 */
public record TransferRequest(

        @NotNull UUID fromAccountId,

        @NotNull UUID toAccountId,

        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}
