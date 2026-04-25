package com.mnco.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for the 'console_access_log' table.
 * Tracks user access to lab nodes.
 */
@Entity
@Table(name = "console_access_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsoleAccessLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private LabInstanceJpaEntity instance;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @CreationTimestamp
    @Column(name = "accessed_at", nullable = false, updatable = false)
    private Instant accessedAt;
}
