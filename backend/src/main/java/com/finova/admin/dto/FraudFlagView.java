package com.finova.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.finova.fraud.FraudFlag;

public record FraudFlagView(
        UUID id,
        UUID accountId,
        String reason,
        String severity,
        String details,
        boolean resolved,
        Instant createdAt
) {
    public static FraudFlagView from(FraudFlag flag) {
        return new FraudFlagView(
                flag.getPublicId(),
                flag.getAccount().getPublicId(),
                flag.getReason().name(),
                flag.getSeverity(),
                flag.getDetails(),
                flag.isResolved(),
                flag.getCreatedAt()
        );
    }
}
