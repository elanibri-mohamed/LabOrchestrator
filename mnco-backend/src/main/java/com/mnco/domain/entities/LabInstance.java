package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * A per-user private copy of an EVE-NG template.
 * Instances are created lazily on first start and reused on subsequent starts.
 * Each instance maps to a separate .unl file under EVE-NG's instances directory.
 */
public class LabInstance {

    private UUID id;
    private UUID templateId;
    private UUID userId;
    private String evengInstancePath;
    private InstanceStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant stoppedAt;

    public LabInstance() {}

    private LabInstance(Builder builder) {
        this.id = builder.id;
        this.templateId = builder.templateId;
        this.userId = builder.userId;
        this.evengInstancePath = builder.evengInstancePath;
        this.status = builder.status != null ? builder.status : InstanceStatus.STOPPED;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.startedAt = builder.startedAt;
        this.stoppedAt = builder.stoppedAt;
    }

    // Domain behaviour
    public boolean isRunning() {
        return InstanceStatus.RUNNING.equals(this.status);
    }

    public boolean isStopped() {
        return InstanceStatus.STOPPED.equals(this.status);
    }

    public boolean isError() {
        return InstanceStatus.ERROR.equals(this.status);
    }

    public void markRunning() {
        this.status = InstanceStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markStopped() {
        this.status = InstanceStatus.STOPPED;
        this.stoppedAt = Instant.now();
    }

    public void markError() {
        this.status = InstanceStatus.ERROR;
    }

    public void reset() {
        this.status = InstanceStatus.STOPPED;
        this.startedAt = null;
        this.stoppedAt = null;
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEvengInstancePath() { return evengInstancePath; }
    public void setEvengInstancePath(String evengInstancePath) { this.evengInstancePath = evengInstancePath; }

    public InstanceStatus getStatus() { return status; }
    public void setStatus(InstanceStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID templateId;
        private UUID userId;
        private String evengInstancePath;
        private InstanceStatus status;
        private Instant createdAt;
        private Instant startedAt;
        private Instant stoppedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder templateId(UUID templateId) { this.templateId = templateId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder evengInstancePath(String path) { this.evengInstancePath = path; return this; }
        public Builder status(InstanceStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant created) { this.createdAt = created; return this; }
        public Builder startedAt(Instant started) { this.startedAt = started; return this; }
        public Builder stoppedAt(Instant stopped) { this.stoppedAt = stopped; return this; }

        public LabInstance build() { return new LabInstance(this); }
    }
}
