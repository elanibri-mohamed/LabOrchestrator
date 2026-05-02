package com.mnco.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for the 'lab_assignments' table.
 * Grants access to a lab template for a specific user.
 */
@Entity
@Table(name = "lab_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"template_id", "user_id"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private LabTemplateJpaEntity template;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
}
