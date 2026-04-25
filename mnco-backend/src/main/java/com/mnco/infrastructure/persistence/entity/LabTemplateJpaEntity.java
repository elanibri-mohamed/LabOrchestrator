package com.mnco.infrastructure.persistence.entity;

import com.mnco.domain.entities.LabTemplateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for the 'lab_templates' table.
 * Represents master topologies synced from EVE-NG.
 */
@Entity
@Table(name = "lab_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTemplateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "eve_template_path", nullable = false, unique = true, length = 512)
    private String eveTemplatePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabTemplateStatus status;

    @Column(name = "cpu_allocated", nullable = false)
    private int cpuAllocated;

    @Column(name = "ram_allocated", nullable = false)
    private int ramAllocated;

    @Column(name = "storage_allocated", nullable = false)
    private int storageAllocated;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
