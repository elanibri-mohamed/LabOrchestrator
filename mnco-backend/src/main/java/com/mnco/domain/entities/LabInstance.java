package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a private copy of a lab template for a specific user.
 */
public class LabInstance {

    private UUID id;
    private UUID templateId;
    private UUID userId;
    private String eveInstancePath;
    private InstanceStatus status;
    private Instant startedAt;
    private Instant stoppedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public LabInstance() {}

    private LabInstance(Builder builder) {
        this.id = builder.id;
        this.templateId = builder.templateId;
        this.userId = builder.userId;
        this.eveInstancePath = builder.eveInstancePath;
        this.status = builder.status;
        this.startedAt = builder.startedAt;
        this.stoppedAt = builder.stoppedAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public boolean isRunning() {
        return InstanceStatus.RUNNING.equals(this.status);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEveInstancePath() { return eveInstancePath; }
    public void setEveInstancePath(String eveInstancePath) { this.eveInstancePath = eveInstancePath; }

    public InstanceStatus getStatus() { return status; }
    public void setStatus(InstanceStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID templateId;
        private UUID userId;
        private String eveInstancePath;
        private InstanceStatus status = InstanceStatus.STOPPED;
        private Instant startedAt;
        private Instant stoppedAt;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder templateId(UUID templateId) { this.templateId = templateId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder eveInstancePath(String path) { this.eveInstancePath = path; return this; }
        public Builder status(InstanceStatus status) { this.status = status; return this; }
        public Builder startedAt(Instant startedAt) { this.startedAt = startedAt; return this; }
        public Builder stoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public LabInstance build() { return new LabInstance(this); }
    }
}
