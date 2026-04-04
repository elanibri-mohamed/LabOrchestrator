package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the hardware resource allocation for a single lab.
 */
public class Resource {

    private UUID id;
    private UUID labId;
    private int cpu;
    private int ram;
    private int storage;
    private Instant createdAt;

    public Resource() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLabId() { return labId; }
    public void setLabId(UUID labId) { this.labId = labId; }

    public int getCpu() { return cpu; }
    public void setCpu(int cpu) { this.cpu = cpu; }

    public int getRam() { return ram; }
    public void setRam(int ram) { this.ram = ram; }

    public int getStorage() { return storage; }
    public void setStorage(int storage) { this.storage = storage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
