package com.mnco.infrastructure.persistence.entity;

import com.mnco.domain.entities.LabTemplateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for eveng_templates table.
 * Represents an EVE-NG lab template synced from the EVE-NG server.
 */
@Entity
@Table(name = "eveng_templates", indexes = {
        @Index(name = "idx_eveng_templates_name", columnList = "name"),
        @Index(name = "idx_eveng_templates_path", columnList = "eveng_template_path"),
        @Index(name = "idx_eveng_templates_status", columnList = "template_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvengTemplateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "eveng_template_path", nullable = false, length = 512, unique = true)
    private String evengTemplatePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_status", nullable = false, length = 10)
    private LabTemplateStatus templateStatus;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(columnDefinition = "TEXT")
    private String description;
}
