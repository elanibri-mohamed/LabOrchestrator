package com.mnco.infrastructure.persistence.entity;

import com.mnco.domain.entities.AuditLog;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable JPA entity for the audit_logs table.
 * @Immutable prevents Hibernate from issuing any UPDATE statement on this entity.
 */
@Entity
@Immutable
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor_id",  columnList = "actor_id"),
        @Index(name = "idx_audit_lab_id",    columnList = "lab_id"),
        @Index(name = "idx_audit_created_at",columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30, updatable = false)
    private AuditLog.EventType eventType;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_username", length = 100, updatable = false)
    private String actorUsername;

    @Column(name = "lab_id", updatable = false)
    private UUID labId;

    @Column(name = "lab_name", length = 100, updatable = false)
    private String labName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private AuditLog.Result result;

    @Column(name = "error_code", length = 100, updatable = false)
    private String errorCode;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 255, updatable = false)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
