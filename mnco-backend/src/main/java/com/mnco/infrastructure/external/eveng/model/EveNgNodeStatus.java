package com.mnco.infrastructure.external.eveng.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a node status object returned by the EVE-NG REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EveNgNodeStatus(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("status") int status,   // 0=stopped, 2=running
        @JsonProperty("type") String type,
        @JsonProperty("cpu") int cpu,
        @JsonProperty("ram") int ram
) {
    public boolean isRunning() { return status == 2; }
    public boolean isStopped() { return status == 0; }
}
