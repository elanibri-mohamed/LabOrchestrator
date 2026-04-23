package com.mnco.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for console_access_log table.
 * Tracks each console URL retrieval for progress monitoring.
 */
@Entity
@Table(name = "console_access_log", indexes = {
        @Index(name = "idx_console_log_instance", columnList = "instance_id"),
        @Index(name = "idx_console_log_user", columnList = "user_id"),
        @Index(name = "idx_console_log_accessed", columnList = "accessed_at DESC")
})
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

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @CreationTimestamp
    @Column(name = "accessed_at", nullable = false, updatable = false)
    private Instant accessedAt;
}
