package com.mnco.infrastructure.persistence.entity;

import com.mnco.domain.entities.InstanceStatus;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for the 'lab_instances' table.
 * Represents a private copy of a lab template for a specific user.
 */
@Entity
@Table(name = "lab_instances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"template_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabInstanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private LabTemplateJpaEntity template;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "eve_instance_path", nullable = false, unique = true, length = 512)
    private String eveInstancePath;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "instance_status")
    private InstanceStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
