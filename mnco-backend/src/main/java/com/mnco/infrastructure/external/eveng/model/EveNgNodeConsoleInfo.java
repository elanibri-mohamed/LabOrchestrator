package com.mnco.infrastructure.external.eveng.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Console connection details for a node in an EVE-NG lab.
 * Returned by {@link com.mnco.infrastructure.external.eveng.EveNgService#getNodeConsoleInfo(String, String)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EveNgNodeConsoleInfo(
        @JsonProperty("protocol") String protocol,
        @JsonProperty("host") String host,
        @JsonProperty("port") int port,
        @JsonProperty("webSocketUrl") String webSocketUrl,
        @JsonProperty("nodeId") String nodeId,
        @JsonProperty("nodeName") String nodeName,
        @JsonProperty("status") String status
) {}
