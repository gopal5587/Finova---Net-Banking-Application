package com.finova.audit;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single audit event. Marked {@link Immutable} so Hibernate will never issue UPDATE/DELETE for it:
 * once written, an audit record is permanent, which is what makes the trail trustworthy.
 */
@Entity
@Table(name = "audit_log")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "event_time", nullable = false, updatable = false)
    private Instant eventTime;

    @Column(nullable = false, updatable = false, length = 50)
    private String username;

    @Column(nullable = false, updatable = false, length = 60)
    private String action;

    @Column(name = "target_type", updatable = false, length = 60)
    private String targetType;

    @Column(nullable = false, updatable = false, length = 10)
    private String outcome;

    @Column(updatable = false, length = 1000)
    private String detail;

    @PrePersist
    void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (eventTime == null) {
            eventTime = Instant.now();
        }
    }
}
