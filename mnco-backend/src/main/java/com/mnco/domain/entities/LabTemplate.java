package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a reusable, versioned network topology template.
 * Templates can be deployed by students/researchers to create new Lab instances.
 */
public class LabTemplate {

    private UUID id;
    private String name;
    private String description;
    private String topologyYaml;
    private String version;
    private UUID authorId;
    private boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;

    public LabTemplate() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopologyYaml() { return topologyYaml; }
    public void setTopologyYaml(String topologyYaml) { this.topologyYaml = topologyYaml; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
