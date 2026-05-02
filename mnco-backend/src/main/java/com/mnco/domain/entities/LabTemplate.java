package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a master lab topology (template) synced from EVE-NG.
 */
public class LabTemplate {

    private UUID id;
    private String name;
    private String description;
    private String eveTemplatePath;
    private LabTemplateStatus status;
    private int cpuAllocated;
    private int ramAllocated;
    private int storageAllocated;
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public LabTemplate() {}

    private LabTemplate(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.eveTemplatePath = builder.eveTemplatePath;
        this.status = builder.status;
        this.cpuAllocated = builder.cpuAllocated;
        this.ramAllocated = builder.ramAllocated;
        this.storageAllocated = builder.storageAllocated;
        this.lastSyncedAt = builder.lastSyncedAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public boolean isActive() {
        return LabTemplateStatus.ACTIVE.equals(this.status);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEveTemplatePath() { return eveTemplatePath; }
    public void setEveTemplatePath(String eveTemplatePath) { this.eveTemplatePath = eveTemplatePath; }

    public LabTemplateStatus getStatus() { return status; }
    public void setStatus(LabTemplateStatus status) { this.status = status; }

    public int getCpuAllocated() { return cpuAllocated; }
    public void setCpuAllocated(int cpuAllocated) { this.cpuAllocated = cpuAllocated; }

    public int getRamAllocated() { return ramAllocated; }
    public void setRamAllocated(int ramAllocated) { this.ramAllocated = ramAllocated; }

    public int getStorageAllocated() { return storageAllocated; }
    public void setStorageAllocated(int storageAllocated) { this.storageAllocated = storageAllocated; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

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
        private String eveTemplatePath;
        private LabTemplateStatus status = LabTemplateStatus.ACTIVE;
        private int cpuAllocated;
        private int ramAllocated;
        private int storageAllocated;
        private Instant lastSyncedAt;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder eveTemplatePath(String path) { this.eveTemplatePath = path; return this; }
        public Builder status(LabTemplateStatus status) { this.status = status; return this; }
        public Builder cpuAllocated(int cpu) { this.cpuAllocated = cpu; return this; }
        public Builder ramAllocated(int ram) { this.ramAllocated = ram; return this; }
        public Builder storageAllocated(int storage) { this.storageAllocated = storage; return this; }
        public Builder lastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public LabTemplate build() { return new LabTemplate(this); }
    }
}
