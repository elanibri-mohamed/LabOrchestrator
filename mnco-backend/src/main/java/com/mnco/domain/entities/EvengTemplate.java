package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain entity representing an EVE-NG lab template.
 * Templates are .unl files synced from the EVE-NG server's templates directory.
 * They serve as the master copy from which per-user instances are created.
 */
public class EvengTemplate {

    private UUID id;
    private String name;
    private String evengTemplatePath;
    private LabTemplateStatus status;
    private Instant lastSyncedAt;
    private Instant createdAt;
    private String description;

    public EvengTemplate() {}

    private EvengTemplate(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.evengTemplatePath = builder.evengTemplatePath;
        this.status = builder.status != null ? builder.status : LabTemplateStatus.ACTIVE;
        this.lastSyncedAt = builder.lastSyncedAt != null ? builder.lastSyncedAt : Instant.now();
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.description = builder.description;
    }

    // Domain behaviour
    public boolean isActive() {
        return LabTemplateStatus.ACTIVE.equals(this.status);
    }

    public void markRemoved() {
        this.status = LabTemplateStatus.REMOVED;
    }

    public void markActive() {
        this.status = LabTemplateStatus.ACTIVE;
    }

    public boolean isRemoved() {
        return LabTemplateStatus.REMOVED.equals(this.status);
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEvengTemplatePath() { return evengTemplatePath; }
    public void setEvengTemplatePath(String evengTemplatePath) { this.evengTemplatePath = evengTemplatePath; }

    public LabTemplateStatus getStatus() { return status; }
    public void setStatus(LabTemplateStatus status) { this.status = status; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String name;
        private String evengTemplatePath;
        private LabTemplateStatus status;
        private Instant lastSyncedAt;
        private Instant createdAt;
        private String description;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder evengTemplatePath(String path) { this.evengTemplatePath = path; return this; }
        public Builder status(LabTemplateStatus status) { this.status = status; return this; }
        public Builder lastSyncedAt(Instant ts) { this.lastSyncedAt = ts; return this; }
        public Builder createdAt(Instant ts) { this.createdAt = ts; return this; }
        public Builder description(String desc) { this.description = desc; return this; }

        public EvengTemplate build() { return new EvengTemplate(this); }
    }
}
