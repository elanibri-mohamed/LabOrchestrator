package com.mnco.infrastructure.external.eveng;

import com.fasterxml.jackson.databind.JsonNode;
import com.mnco.domain.entities.Lab;
import com.mnco.exception.custom.EveNgIntegrationException;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Production EVE-NG REST API client (API v2).
 * Activated when eveng.simulation-mode=false (default).
 *
 * All operations authenticate via EVE-NG session cookie per call.
 * Implements: create, start, stop, delete, clone (FR-LM-06), console (FR-LM-09).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "eveng.simulation-mode", havingValue = "false", matchIfMissing = true)
public class EveNgRestService implements EveNgService {

    private final WebClient webClient;
    private final String username;
    private final String password;
    private final String labBasePath;
    private final String evengHost;

    public EveNgRestService(
            @Value("${eveng.base-url}") String baseUrl,
            @Value("${eveng.username}") String username,
            @Value("${eveng.password}") String password,
            @Value("${eveng.lab-base-path:/opt/unetlab/labs}") String labBasePath) {

        this.username = username;
        this.password = password;
        this.labBasePath = labBasePath;
        this.evengHost = baseUrl.replaceFirst("https?://", "");

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── Session ───────────────────────────────────────────────────────────────

    private String authenticate() {
        try {
            var response = webClient.post()
                    .uri("/api/auth/login")
                    .bodyValue(Map.of("username", username, "password", password, "html5", -1))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.getHeaders().get("Set-Cookie") == null) {
                throw new EveNgIntegrationException("EVE-NG login failed: no session cookie");
            }
            return response.getHeaders().get("Set-Cookie").stream()
                    .filter(c -> c.startsWith("unetlab"))
                    .findFirst()
                    .map(c -> c.split(";")[0])
                    .orElseThrow(() -> new EveNgIntegrationException("unetlab cookie missing"));
        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException("EVE-NG auth failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public EveNgLabResult createTopology(Lab lab) {
        log.info("Creating EVE-NG topology for lab '{}'", lab.getName());
        String cookie = authenticate();
        String labFileName = sanitize(lab.getName()) + "-" + lab.getId();

        try {
            webClient.post().uri("/api/labs")
                    .header("Cookie", cookie)
                    .bodyValue(Map.of("path", "/", "name", labFileName,
                            "version", "1", "description",
                            lab.getDescription() != null ? lab.getDescription() : ""))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            String evengLabId = "/" + labFileName + ".unl";
            log.info("EVE-NG topology created: '{}'", evengLabId);
            return new EveNgLabResult(evengLabId, null, labBasePath + evengLabId, "created");
        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException("Create topology failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    @Override
    public void startLab(String evengLabId) {
        log.info("Starting all nodes in EVE-NG lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            webClient.get()
                    .uri("/api/labs{id}/nodes", evengLabId)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .flatMap(nodes -> webClient.put()
                            .uri("/api/labs{id}/nodes/start", evengLabId)
                            .header("Cookie", cookie)
                            .retrieve()
                            .bodyToMono(Void.class))
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException("Start lab failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    @Override
    public void stopLab(String evengLabId) {
        log.info("Stopping all nodes in EVE-NG lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            webClient.put()
                    .uri("/api/labs{id}/nodes/stop", evengLabId)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException("Stop lab failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deleteLab(String evengLabId) {
        log.info("Deleting EVE-NG lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            webClient.delete()
                    .uri("/api/labs{id}", evengLabId)
                    .header("Cookie", cookie)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException("Delete lab failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    // ── Clone (FR-LM-06) ──────────────────────────────────────────────────────

    @Override
    public EveNgCloneResult cloneLab(String sourceEvengLabId, String cloneName, String cloneId) {
        log.info("Cloning EVE-NG lab '{}' → '{}'", sourceEvengLabId, cloneName);
        String cookie = authenticate();

        // EVE-NG does not have a native clone API — we move via export+import.
        // Step 1: export the source lab as a .unl file
        // Step 2: POST it to the labs endpoint with the new name
        // This is a simplified implementation; production would stream the file bytes.
        String cloneFileName = sanitize(cloneName) + "-" + cloneId;

        try {
            // Export source
            byte[] exportedLab = webClient.get()
                    .uri("/api/labs{id}/export", sourceEvengLabId)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (exportedLab == null || exportedLab.length == 0) {
                throw new EveNgIntegrationException("Export returned empty content");
            }

            // Import as new lab
            webClient.post()
                    .uri("/api/labs/import")
                    .header("Cookie", cookie)
                    .header("Content-Type", "application/octet-stream")
                    .bodyValue(exportedLab)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            String clonedPath = "/" + cloneFileName + ".unl";
            log.info("EVE-NG clone created at '{}'", clonedPath);
            return new EveNgCloneResult(clonedPath, labBasePath + clonedPath, "cloned");
        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException("Clone lab failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    // ── Node Statuses ─────────────────────────────────────────────────────────

    @Override
    public List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId) {
        log.debug("Fetching node statuses for lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            JsonNode response = webClient.get()
                    .uri("/api/labs{id}/nodes", evengLabId)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null || !response.has("data")) return Collections.emptyList();

            List<EveNgNodeStatus> statuses = new ArrayList<>();
            response.get("data").fields().forEachRemaining(entry -> {
                JsonNode n = entry.getValue();
                statuses.add(new EveNgNodeStatus(
                        n.path("id").asText(), n.path("name").asText(),
                        n.path("status").asInt(), n.path("type").asText(),
                        n.path("cpu").asInt(), n.path("ram").asInt()));
            });
            return statuses;
        } catch (WebClientResponseException ex) {
            log.warn("Failed to get node statuses for '{}': {}", evengLabId, ex.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Console Info (FR-LM-09) ───────────────────────────────────────────────

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId) {
        log.debug("Fetching console info for node='{}' lab='{}'", nodeId, evengLabId);
        String cookie = authenticate();
        try {
            JsonNode response = webClient.get()
                    .uri("/api/labs{labId}/nodes/{nodeId}", evengLabId, nodeId)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || !response.has("data")) {
                throw new EveNgIntegrationException("Empty response for node console info");
            }

            JsonNode data = response.get("data");
            String nodeType = data.path("type").asText("iol");
            int consolePort = data.path("console").asInt(0);
            String nodeName  = data.path("name").asText("node-" + nodeId);
            int statusCode   = data.path("status").asInt(0);
            String status    = statusCode == 2 ? "RUNNING" : "STOPPED";

            // EVE-NG assigns Telnet ports in the 32xxx range; VNC in 5900+node offset
            String protocol;
            String wsUrl = null;
            if (nodeType.contains("qemu") || nodeType.contains("docker")) {
                protocol = "VNC";
            } else {
                protocol = "TELNET";
                wsUrl = String.format("ws://%s:8080/api/labs%s/nodes/%s/console",
                        evengHost, evengLabId, nodeId);
            }

            return new EveNgNodeConsoleInfo(
                    protocol, evengHost, consolePort, wsUrl,
                    nodeId, nodeName, status);

        } catch (WebClientResponseException ex) {
            throw new EveNgIntegrationException(
                    "Get console info failed: HTTP " + ex.getStatusCode(), ex);
        }
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
    }
}
