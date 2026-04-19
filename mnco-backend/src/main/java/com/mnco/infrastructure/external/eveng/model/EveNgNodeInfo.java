package com.mnco.infrastructure.external.eveng.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node information retrieved from EVE-NG server.
 * Includes resource specifications for CPU, RAM, and storage allocation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EveNgNodeInfo(
        @JsonProperty("id") String id,              // Node identifier within lab
        @JsonProperty("name") String name,          // Node name (e.g., "R1", "SW1")
        @JsonProperty("type") String type,          // Device type (qemu, docker, vios, etc)
        @JsonProperty("status") Integer status,     // 0=stopped, 2=running, etc
        @JsonProperty("cpu") Integer cpu,           // CPU cores allocated to node
        @JsonProperty("ram") Integer ram,           // RAM in MB allocated to node
        @JsonProperty("nvram") Integer nvram,       // NVRAM size in KB
        @JsonProperty("disk") Integer disk,         // Disk size in GB
        @JsonProperty("image") String image,        // Image/template used
        @JsonProperty("console") String console     // Console type (telnet, vnc, spice, etc)
) {

    /**
     * Check if node is currently running.
     */
    public boolean isRunning() {
        return status != null && status == 2;
    }

    /**
     * Check if node is stopped.
     */
    public boolean isStopped() {
        return status != null && status == 0;
    }

    /**
     * Get RAM in GB (converted from MB).
     */
    public int getRamGb() {
        if (ram == null) return 0;
        return Math.max(1, ram / 1024);
    }

    /**
     * Get disk size in GB, or default to 0.
     */
    public int getDiskGb() {
        return disk != null ? disk : 0;
    }

    /**
     * Get CPU count, or default to 1.
     */
    public int getCpuCount() {
        return cpu != null ? cpu : 1;
    }
}
