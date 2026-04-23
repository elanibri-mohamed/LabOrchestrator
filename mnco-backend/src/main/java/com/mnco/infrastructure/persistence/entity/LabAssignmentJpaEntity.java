package com.mnco.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for lab_assignments table.
 * Represents an assignment grant: a user may access a specific EVE-NG template.
 */
@Entity
@Table(name = "lab_assignments", indexes = {
        @Index(name = "idx_assignments_user", columnList = "user_id"),
        @Index(name = "idx_assignments_template", columnList = "template_id"),
        @Index(name = "idx_assignments_assigned_by", columnList = "assigned_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabAssignmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
}
