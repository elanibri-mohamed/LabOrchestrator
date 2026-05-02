package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Grants access to a lab template for a specific user.
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
        this.assignedAt = builder.assignedAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

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

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID templateId;
        private UUID userId;
        private UUID assignedBy;
        private Instant assignedAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder templateId(UUID templateId) { this.templateId = templateId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder assignedBy(UUID assignedBy) { this.assignedBy = assignedBy; return this; }
        public Builder assignedAt(Instant assignedAt) { this.assignedAt = assignedAt; return this; }

        public LabAssignment build() { return new LabAssignment(this); }
    }
}
