package com.finova.account.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cached balance snapshot. Implements {@link Serializable} because instances are stored in Redis.
 */
public record BalanceResponse(
        UUID accountId,
        String currency,
        BigDecimal balance
) implements Serializable {
}
