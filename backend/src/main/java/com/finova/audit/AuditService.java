package com.finova.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit events. Uses {@link Propagation#REQUIRES_NEW} so a failed business
 * transaction cannot roll back the audit row that records that failure.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(String actor,
                           String action,
                           String resourceType,
                           String resourceId,
                           AuditOutcome outcome,
                           String details,
                           String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setOutcome(outcome);
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);

        AuditLog saved = auditLogRepository.save(entry);
        log.info("audit action={} actor={} outcome={} resource={}:{}",
                action, actor, outcome, resourceType, resourceId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listForActor(String actor, Pageable pageable) {
        return auditLogRepository.findByActorOrderByCreatedAtDesc(actor, pageable);
    }
}
