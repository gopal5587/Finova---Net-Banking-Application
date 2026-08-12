package com.finova.transaction.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Shared shape for single-account operations (deposit and withdrawal). The target account is taken
 * from the URL path, so only the amount and an optional note are supplied in the body.
 */
public record MoneyRequest(

        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}
