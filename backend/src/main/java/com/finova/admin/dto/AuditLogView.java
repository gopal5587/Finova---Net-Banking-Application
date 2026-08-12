package com.finova.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.finova.audit.AuditLog;

public record AuditLogView(
        UUID id,
        Instant eventTime,
        String username,
        String action,
        String targetType,
        String outcome,
        String detail
) {
    public static AuditLogView from(AuditLog log) {
        return new AuditLogView(
                log.getPublicId(),
                log.getEventTime(),
                log.getUsername(),
                log.getAction(),
                log.getTargetType(),
                log.getOutcome(),
                log.getDetail()
        );
    }
}
