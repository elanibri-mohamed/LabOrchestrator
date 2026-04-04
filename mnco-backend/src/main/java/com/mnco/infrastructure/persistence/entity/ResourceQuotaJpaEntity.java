package com.mnco.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the resource_quotas table.
 * One row per user, enforced by the UNIQUE constraint on user_id.
 */
@Entity
@Table(name = "resource_quotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceQuotaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "max_labs", nullable = false)
    private int maxLabs = 3;

    @Column(name = "max_cpu", nullable = false)
    private int maxCpu = 8;

    @Column(name = "max_ram_gb", nullable = false)
    private int maxRamGb = 16;

    @Column(name = "max_storage_gb", nullable = false)
    private int maxStorageGb = 50;

    @Column(name = "used_labs", nullable = false)
    private int usedLabs = 0;

    @Column(name = "used_cpu", nullable = false)
    private int usedCpu = 0;

    @Column(name = "used_ram_gb", nullable = false)
    private int usedRamGb = 0;

    @Column(name = "used_storage_gb", nullable = false)
    private int usedStorageGb = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
