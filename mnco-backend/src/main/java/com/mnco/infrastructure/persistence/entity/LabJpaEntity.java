package com.mnco.infrastructure.persistence.entity;

import com.mnco.domain.entities.LabStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for the 'labs' table.
 */
@Entity
@Table(name = "labs", indexes = {
        @Index(name = "idx_labs_owner_id", columnList = "owner_id"),
        @Index(name = "idx_labs_status", columnList = "status"),
        @Index(name = "idx_labs_eveng_lab_id", columnList = "eveng_lab_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabStatus status;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "eveng_lab_id", length = 255)
    private String evengLabId;

    @Column(name = "eveng_node_id", length = 255)
    private String evengNodeId;

    @Column(name = "cpu_allocated", nullable = false)
    private int cpuAllocated;

    @Column(name = "ram_allocated", nullable = false)
    private int ramAllocated;

    @Column(name = "storage_allocated", nullable = false)
    private int storageAllocated;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
