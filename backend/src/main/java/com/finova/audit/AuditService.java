package com.finova.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit records. Writes run in a {@code REQUIRES_NEW} transaction so an audit entry is
 * committed independently of the business operation - crucially, a FAILURE audit still survives
 * even when the failing business transaction rolls back.
 */
@Service
public class AuditService {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";

    private static final int MAX_DETAIL_LENGTH = 1000;

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String action, String targetType, String outcome, String detail) {
        AuditLog entry = new AuditLog();
        entry.setUsername(username);
        entry.setAction(action);
        entry.setTargetType(emptyToNull(targetType));
        entry.setOutcome(outcome);
        entry.setDetail(truncate(detail));
        auditLogRepository.save(entry);
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= MAX_DETAIL_LENGTH ? detail : detail.substring(0, MAX_DETAIL_LENGTH);
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
