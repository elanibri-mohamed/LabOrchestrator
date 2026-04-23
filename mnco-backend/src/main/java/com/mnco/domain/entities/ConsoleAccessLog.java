package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for console access events.
 * Each time a user fetches a console URL, a row is inserted for progress tracking.
 */
public class ConsoleAccessLog {

    private UUID id;
    private UUID userId;
    private UUID instanceId;
    private String nodeId;
    private Instant accessedAt;

    public ConsoleAccessLog() {}

    private ConsoleAccessLog(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.instanceId = builder.instanceId;
        this.nodeId = builder.nodeId;
        this.accessedAt = builder.accessedAt != null ? builder.accessedAt : Instant.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Instant getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Instant accessedAt) { this.accessedAt = accessedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID userId;
        private UUID instanceId;
        private String nodeId;
        private Instant accessedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder instanceId(UUID instanceId) { this.instanceId = instanceId; return this; }
        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder accessedAt(Instant accessedAt) { this.accessedAt = accessedAt; return this; }

        public ConsoleAccessLog build() { return new ConsoleAccessLog(this); }
    }
}
