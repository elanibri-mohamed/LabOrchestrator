package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain entity representing a virtual laboratory environment.
 * Encapsulates the lifecycle state and EVE-NG binding for a lab.
 */
public class Lab {

    private UUID id;
    private String name;
    private String description;
    private LabStatus status;
    private UUID ownerId;
    private UUID templateId;
    private String evengLabId;
    private String evengNodeId;
    private int cpuAllocated;
    private int ramAllocated;
    private int storageAllocated;
    private Instant startedAt;
    private Instant stoppedAt;
    private Instant lastActiveAt;
    private Instant createdAt;
    private Instant updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Lab() {}

    private Lab(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.ownerId = builder.ownerId;
        this.templateId = builder.templateId;
        this.evengLabId = builder.evengLabId;
        this.evengNodeId = builder.evengNodeId;
        this.cpuAllocated = builder.cpuAllocated;
        this.ramAllocated = builder.ramAllocated;
        this.storageAllocated = builder.storageAllocated;
        this.startedAt = builder.startedAt;
        this.stoppedAt = builder.stoppedAt;
        this.lastActiveAt = builder.lastActiveAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean isRunning() {
        return LabStatus.RUNNING.equals(this.status);
    }

    public boolean isStopped() {
        return LabStatus.STOPPED.equals(this.status);
    }

    public boolean isDeletable() {
        return LabStatus.STOPPED.equals(this.status)
                || LabStatus.ERROR.equals(this.status)
                || LabStatus.PENDING.equals(this.status);
    }

    public boolean isStartable() {
        return LabStatus.STOPPED.equals(this.status)
                || LabStatus.PENDING.equals(this.status);
    }

    public boolean isStoppable() {
        return LabStatus.RUNNING.equals(this.status);
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }

    public void markStarted() {
        this.status = LabStatus.RUNNING;
        this.startedAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    public void markStopped() {
        this.status = LabStatus.STOPPED;
        this.stoppedAt = Instant.now();
    }

    public void markError() {
        this.status = LabStatus.ERROR;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LabStatus getStatus() { return status; }
    public void setStatus(LabStatus status) { this.status = status; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getEvengLabId() { return evengLabId; }
    public void setEvengLabId(String evengLabId) { this.evengLabId = evengLabId; }

    public String getEvengNodeId() { return evengNodeId; }
    public void setEvengNodeId(String evengNodeId) { this.evengNodeId = evengNodeId; }

    public int getCpuAllocated() { return cpuAllocated; }
    public void setCpuAllocated(int cpuAllocated) { this.cpuAllocated = cpuAllocated; }

    public int getRamAllocated() { return ramAllocated; }
    public void setRamAllocated(int ramAllocated) { this.ramAllocated = ramAllocated; }

    public int getStorageAllocated() { return storageAllocated; }
    public void setStorageAllocated(int storageAllocated) { this.storageAllocated = storageAllocated; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; }

    public Instant getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String name;
        private String description;
        private LabStatus status = LabStatus.PENDING;
        private UUID ownerId;
        private UUID templateId;
        private String evengLabId;
        private String evengNodeId;
        private int cpuAllocated;
        private int ramAllocated;
        private int storageAllocated;
        private Instant startedAt;
        private Instant stoppedAt;
        private Instant lastActiveAt;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(LabStatus status) { this.status = status; return this; }
        public Builder ownerId(UUID ownerId) { this.ownerId = ownerId; return this; }
        public Builder templateId(UUID templateId) { this.templateId = templateId; return this; }
        public Builder evengLabId(String evengLabId) { this.evengLabId = evengLabId; return this; }
        public Builder evengNodeId(String evengNodeId) { this.evengNodeId = evengNodeId; return this; }
        public Builder cpuAllocated(int cpu) { this.cpuAllocated = cpu; return this; }
        public Builder ramAllocated(int ram) { this.ramAllocated = ram; return this; }
        public Builder storageAllocated(int storage) { this.storageAllocated = storage; return this; }
        public Builder startedAt(Instant startedAt) { this.startedAt = startedAt; return this; }
        public Builder stoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; return this; }
        public Builder lastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Lab build() { return new Lab(this); }
    }
}
