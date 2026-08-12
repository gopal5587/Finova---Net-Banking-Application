package com.finova.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Records an audit entry around every {@link Auditable} method.
 *
 * <p>{@code @Order(0)} makes this aspect the outermost advice, so it wraps Spring's transaction
 * interceptor: by the time {@code proceed()} returns, the business transaction has already committed,
 * so a SUCCESS audit truly means the work stuck. Failures are audited too (then the exception is
 * rethrown so normal error handling is unaffected).
 */
@Aspect
@Component
@Order(0)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final String SYSTEM_ACTOR = "system";

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String username = currentUsername();
        try {
            Object result = joinPoint.proceed();
            safeRecord(username, auditable, AuditService.OUTCOME_SUCCESS, null);
            return result;
        } catch (Throwable ex) {
            safeRecord(username, auditable, AuditService.OUTCOME_FAILURE, ex.getMessage());
            throw ex;
        }
    }

    /** Auditing must never break the business flow, so a logging failure here is swallowed (and logged). */
    private void safeRecord(String username, Auditable auditable, String outcome, String detail) {
        try {
            auditService.record(username, auditable.action(), auditable.targetType(), outcome, detail);
        } catch (RuntimeException e) {
            log.error("Failed to write audit entry for action '{}'", auditable.action(), e);
        }
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return SYSTEM_ACTOR;
        }
        return auth.getName();
    }
}
