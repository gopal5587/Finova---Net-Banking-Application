package com.finova.audit;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Custom Envers revision entity mapped to the {@code revinfo} table.
 *
 * <p>Extends the default revision (number + timestamp) with a {@code username} column so every
 * historical change can be attributed to whoever made it - a core requirement for financial
 * compliance. The value is populated by {@link FinovaRevisionListener} from the security context.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(FinovaRevisionListener.class)
@Getter
@Setter
public class FinovaRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq_gen")
    @SequenceGenerator(name = "revinfo_seq_gen", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    private int rev;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "username", length = 50)
    private String username;
}
