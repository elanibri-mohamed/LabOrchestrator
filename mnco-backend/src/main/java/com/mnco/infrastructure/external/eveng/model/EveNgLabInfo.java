package com.mnco.infrastructure.external.eveng.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Lab metadata retrieved from EVE-NG server via /api/labs endpoint.
 * Used for lab discovery and synchronization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EveNgLabInfo(
        @JsonProperty("id") String id,                      // Lab identifier in EVE-NG
        @JsonProperty("name") String name,                  // Lab name
        @JsonProperty("path") String path,                  // Full EVE-NG path (e.g., "/lab-name.unl")
        @JsonProperty("description") String description,    // Lab description
        @JsonProperty("version") String version,            // EVE-NG version
        @JsonProperty("created") Long createdTimestamp,     // Unix timestamp
        @JsonProperty("modified") Long modifiedTimestamp,   // Unix timestamp
        @JsonProperty("status") Integer status,             // Lab status (0=OK, etc)
        @JsonProperty("nodecount") Integer nodeCount        // Number of nodes in lab
) {

    /**
     * Convert Unix timestamp to Instant.
     */
    public Instant getCreatedInstant() {
        return createdTimestamp != null ? Instant.ofEpochSecond(createdTimestamp) : null;
    }

    /**
     * Convert Unix timestamp to Instant.
     */
    public Instant getModifiedInstant() {
        return modifiedTimestamp != null ? Instant.ofEpochSecond(modifiedTimestamp) : null;
    }

    /**
     * Extract lab name from path if name is not available.
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (path != null) {
            // Extract from path like "/lab-name.unl" -> "lab-name"
            return path.replaceAll("^/+", "").replaceAll("\\.unl$", "");
        }
        return "Unknown Lab";
    }
}
