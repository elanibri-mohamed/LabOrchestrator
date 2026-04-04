package com.mnco.infrastructure.external.eveng;

/**
 * Console connection parameters for a virtual node (FR-LM-09).
 *
 * Returned by GET /labs/{id}/nodes/{nodeId}/console
 *
 * The frontend uses these to:
 *   - Launch an embedded terminal (webSocketUrl for TELNET/SSH via websocket)
 *   - Launch an external VNC/SPICE client (host + port)
 */
public record EveNgNodeConsoleInfo(
        String protocol,        // TELNET | VNC | SPICE
        String host,            // IP or FQDN of the EVE-NG server
        int port,               // port number for the protocol
        String webSocketUrl,    // WebSocket URL for browser-based access (may be null for VNC)
        String nodeId,
        String nodeName,
        String nodeStatus       // RUNNING | STOPPED
) {}
