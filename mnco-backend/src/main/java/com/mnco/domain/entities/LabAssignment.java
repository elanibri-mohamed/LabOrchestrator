package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Assignment grant: a user (student) may access a specific EVE-NG template.
 * This is the multi-tenant boundary. No instance creation occurs here;
 * it simply authorizes the user to start an instance later.
 */
public class LabAssignment {

    private UUID id;
    private UUID templateId;
    private UUID userId;
    private UUID assignedBy;
    private Instant assignedAt;

    public LabAssignment() {}

    private LabAssignment(Builder builder) {
        this.id = builder.id;
        this.templateId = builder.templateId;
        this.userId = builder.userId;
        this.assignedBy = builder.assignedBy;
        this.assignedAt = builder.assignedAt != null ? builder.assignedAt : Instant.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID templateId;
        private UUID userId;
        private UUID assignedBy;
        private Instant assignedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder templateId(UUID templateId) { this.templateId = templateId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder assignedBy(UUID assignedBy) { this.assignedBy = assignedBy; return this; }
        public Builder assignedAt(Instant assignedAt) { this.assignedAt = assignedAt; return this; }

        public LabAssignment build() { return new LabAssignment(this); }
    }
}
