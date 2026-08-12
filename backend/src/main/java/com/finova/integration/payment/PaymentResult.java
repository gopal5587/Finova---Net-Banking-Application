package com.finova.integration.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResult(
        String provider,
        String externalReference,
        String status,
        UUID accountId,
        BigDecimal amount,
        String currency
) {
}
