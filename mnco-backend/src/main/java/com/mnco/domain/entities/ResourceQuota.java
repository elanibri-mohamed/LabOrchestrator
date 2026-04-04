package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks per-user resource quotas and current usage.
 * Enforces SRS requirement FR-RM: Resource Management.
 *
 * Design note: usage counters are maintained here for fast quota checks
 * without requiring COUNT queries on the labs table at every operation.
 */
public class ResourceQuota {

    private UUID id;
    private UUID userId;

    // ── Limits ────────────────────────────────────────────────────────────────
    private int maxLabs;
    private int maxCpu;
    private int maxRamGb;
    private int maxStorageGb;

    // ── Current usage ─────────────────────────────────────────────────────────
    private int usedLabs;
    private int usedCpu;
    private int usedRamGb;
    private int usedStorageGb;

    private Instant updatedAt;

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean canAllocate(int cpu, int ramGb, int storageGb) {
        return (usedLabs < maxLabs)
                && (usedCpu + cpu <= maxCpu)
                && (usedRamGb + ramGb <= maxRamGb)
                && (usedStorageGb + storageGb <= maxStorageGb);
    }

    public void allocate(int cpu, int ramGb, int storageGb) {
        this.usedLabs++;
        this.usedCpu += cpu;
        this.usedRamGb += ramGb;
        this.usedStorageGb += storageGb;
        this.updatedAt = Instant.now();
    }

    public void release(int cpu, int ramGb, int storageGb) {
        this.usedLabs = Math.max(0, this.usedLabs - 1);
        this.usedCpu = Math.max(0, this.usedCpu - cpu);
        this.usedRamGb = Math.max(0, this.usedRamGb - ramGb);
        this.usedStorageGb = Math.max(0, this.usedStorageGb - storageGb);
        this.updatedAt = Instant.now();
    }

    public int getRemainingLabs()    { return maxLabs - usedLabs; }
    public int getRemainingCpu()     { return maxCpu - usedCpu; }
    public int getRemainingRamGb()   { return maxRamGb - usedRamGb; }
    public int getRemainingStorage() { return maxStorageGb - usedStorageGb; }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId()            { return id; }
    public void setId(UUID id)     { this.id = id; }

    public UUID getUserId()             { return userId; }
    public void setUserId(UUID userId)  { this.userId = userId; }

    public int getMaxLabs()             { return maxLabs; }
    public void setMaxLabs(int v)       { this.maxLabs = v; }

    public int getMaxCpu()              { return maxCpu; }
    public void setMaxCpu(int v)        { this.maxCpu = v; }

    public int getMaxRamGb()            { return maxRamGb; }
    public void setMaxRamGb(int v)      { this.maxRamGb = v; }

    public int getMaxStorageGb()        { return maxStorageGb; }
    public void setMaxStorageGb(int v)  { this.maxStorageGb = v; }

    public int getUsedLabs()            { return usedLabs; }
    public void setUsedLabs(int v)      { this.usedLabs = v; }

    public int getUsedCpu()             { return usedCpu; }
    public void setUsedCpu(int v)       { this.usedCpu = v; }

    public int getUsedRamGb()           { return usedRamGb; }
    public void setUsedRamGb(int v)     { this.usedRamGb = v; }

    public int getUsedStorageGb()       { return usedStorageGb; }
    public void setUsedStorageGb(int v) { this.usedStorageGb = v; }

    public Instant getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(Instant v)    { this.updatedAt = v; }
}
