package com.mnco.infrastructure.persistence.entity;

import com.mnco.domain.entities.InstanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for lab_instances table.
 * Represents a per-user private copy of an EVE-NG template.
 */
@Entity
@Table(name = "lab_instances", indexes = {
        @Index(name = "idx_instances_user", columnList = "user_id"),
        @Index(name = "idx_instances_template", columnList = "template_id"),
        @Index(name = "idx_instances_status", columnList = "status")
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

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "eveng_instance_path", nullable = false, length = 512, unique = true)
    private String evengInstancePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InstanceStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;
}
