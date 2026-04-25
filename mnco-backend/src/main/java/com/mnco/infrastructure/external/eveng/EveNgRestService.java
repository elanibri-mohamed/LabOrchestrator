package com.mnco.infrastructure.external.eveng;

import com.fasterxml.jackson.databind.JsonNode;
import com.mnco.domain.entities.Lab;
import com.mnco.exception.custom.EveNgIntegrationException;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;

/**
 * Production EVE-NG REST API client (API v2).
 *
 * All operations authenticate via EVE-NG session cookie per call.
 * Implements: create, start, stop, delete, clone (FR-LM-06), console (FR-LM-09).
 * 
 * NOTE: Throws EveNgIntegrationException if EVE-NG server is unreachable.
 * No simulation mode available - errors will propagate to caller.
 */
@Slf4j
@Service
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
            // Ensure there is a slash between 'labs' and the ID
            String uri = "/api/labs/" + evengLabId.replaceFirst("^/", "") + "/nodes/start";
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            webClient.get() // Changed from .put() to .get()
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(String.class) // EVE-NG returns a JSON status msg
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
            // Ensure path starts with /api/labs/ and handles potential leading slash in ID
            String uri = "/api/labs/" + evengLabId.replaceFirst("^/", "") + "/nodes/stop";
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            webClient.get() // Must be GET, not PUT
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(String.class) // EVE-NG returns a JSON status response
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
            String uri = "/api/labs" + evengLabId;
            log.debug("Calling EVE-NG API: DELETE {}", uri);
            
            webClient.delete()
                    .uri(uri)
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
            String uri = "/api/labs" + evengLabId + "/nodes";
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            JsonNode response = webClient.get()
                    .uri(uri)
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
            String uri = "/api/labs" + evengLabId + "/nodes/" + nodeId;
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            JsonNode response = webClient.get()
                    .uri(uri)
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

    // ── Lab Discovery ────────────────────────────────────────────────────────

    @Override
    public List<EveNgLabInfo> getAllLabs() {
        log.info("Fetching all labs from EVE-NG via recursive folder traversal");
        String cookie = authenticate();
        List<EveNgLabInfo> allLabs = new ArrayList<>();
        try {
            traverseFolders("/", cookie, allLabs);
            log.info("Retrieved {} labs from EVE-NG", allLabs.size());
            return allLabs;
        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch labs: HTTP {}", ex.getStatusCode());
            throw new EveNgIntegrationException("Failed to fetch labs: HTTP " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.error("Failed to fetch labs: {}", ex.getMessage());
            throw new EveNgIntegrationException("Failed to fetch labs: " + ex.getMessage(), ex);
        }
    }

    /**
     * Recursively traverse all folders from EVE-NG and collect labs.
     * EVE-NG API structure: /api/folders/{path} returns {folders: [...], labs: [...]}
     */
    private void traverseFolders(String folderPath, String cookie, List<EveNgLabInfo> allLabs) {
        try {
            log.debug("Traversing folder: {}", folderPath);
            
            // Construct URI correctly:
            // - Root "/" → "/api/folders/"
            // - Subfolder "/User1" → "/api/folders/User1"
            String uri = folderPath.equals("/") ? "/api/folders/" : "/api/folders" + folderPath;
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            // Fetch folder contents
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null) {
                log.warn("Null response from EVE-NG for folder: {}", folderPath);
                return;
            }

            log.debug("Response status: {}, has data: {}", 
                    response.path("status").asText("unknown"), response.has("data"));

            if (!response.has("data")) {
                log.warn("No 'data' field in EVE-NG response for folder: {}", folderPath);
                return;
            }

            JsonNode data = response.get("data");
            log.debug("Folder response: folders={}, labs={}", 
                    data.has("folders") ? data.get("folders").size() : 0,
                    data.has("labs") ? data.get("labs").size() : 0);

            // Process labs in this folder
            if (data.has("labs") && data.get("labs").isArray()) {
                for (JsonNode labEntry : data.get("labs")) {
                    String labPath = labEntry.path("path").asText();
                    String labFile = labEntry.path("file").asText();
                    log.debug("Found lab in folder: file='{}', path='{}'", labFile, labPath);
                    
                    try {
                        Optional<EveNgLabInfo> labInfo = getLabByPath(labPath);
                        if (labInfo.isPresent()) {
                            allLabs.add(labInfo.get());
                            log.info("✓ Added lab: {} ({})", labFile, labPath);
                        } else {
                            log.warn("⚠ Lab not found or empty response: {}", labPath);
                        }
                    } catch (Exception ex) {
                        log.warn("✗ Failed to fetch lab details for {}: {}", labPath, ex.getMessage());
                    }
                }
            } else {
                log.debug("No labs in this folder");
            }

            // Recursively process subfolders (skip ".." parent reference)
            if (data.has("folders") && data.get("folders").isArray()) {
                for (JsonNode folderEntry : data.get("folders")) {
                    String subfolderPath = folderEntry.path("path").asText();
                    String folderName = folderEntry.path("name").asText();
                    
                    // Skip parent directory reference
                    if ("..".equals(folderName)) {
                        log.debug("Skipping parent directory (..)");
                        continue;
                    }
                    
                    log.debug("Recursing into subfolder: {} ({})", folderName, subfolderPath);
                    traverseFolders(subfolderPath, cookie, allLabs);
                }
            } else {
                log.debug("No subfolders");
            }
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                log.debug("Folder not found (404): {}", folderPath);
            } else {
                log.warn("Failed to traverse folder {}: HTTP {} - {}", folderPath, ex.getStatusCode(), ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Error traversing folder {}: {}", folderPath, ex.getMessage());
        }
    }

    @Override
    public Optional<EveNgLabInfo> getLabByPath(String evengLabPath) {
        log.debug("Fetching lab details by path: '{}'", evengLabPath);
        String cookie = authenticate();
        try {
            // Construct URI properly: /api/labs + path
            // EVE-NG expects full path like /api/labs/basic-router-switch-lab.unl
            String uri = "/api/labs" + evengLabPath;
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                log.warn("Null response from EVE-NG for lab path: {}", evengLabPath);
                return Optional.empty();
            }

            log.debug("Lab response status: {}, has data: {}", 
                    response.path("status").asText("unknown"), response.has("data"));

            if (!response.has("data")) {
                log.debug("Lab not found: {}", evengLabPath);
                return Optional.empty();
            }

            JsonNode labNode = response.get("data");
            EveNgLabInfo lab = new EveNgLabInfo(
                    labNode.path("id").asText(),
                    labNode.path("name").asText(),
                    evengLabPath,  // Use the path parameter, not from response (EVE-NG doesn't include it)
                    labNode.path("description").asText(),
                    labNode.path("version").asText(),
                    labNode.path("created").asLong(0L),
                    labNode.path("modified").asLong(0L),
                    labNode.path("status").asInt(0),
                    labNode.path("nodecount").asInt(0)
            );
            return Optional.of(lab);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                log.debug("Lab not found: {}", evengLabPath);
                return Optional.empty();
            }
            log.warn("Failed to get lab details: HTTP {}", ex.getStatusCode());
            throw new EveNgIntegrationException(
                    "Failed to get lab details: HTTP " + ex.getStatusCode(), ex);
        }
    }

    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId) {
        log.debug("Fetching nodes for lab: '{}'", evengLabId);
        String cookie = authenticate();
        try {
            // Construct URI properly: /api/labs{path}/nodes
            String uri = "/api/labs" + evengLabId + "/nodes";
            log.debug("Calling EVE-NG API: GET {}", uri);
            
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null || !response.has("data")) {
                log.debug("No nodes data in response for lab: {}", evengLabId);
                return Collections.emptyList();
            }

            List<EveNgNodeInfo> nodes = new ArrayList<>();
            response.get("data").fields().forEachRemaining(entry -> {
                JsonNode nodeData = entry.getValue();
                try {
                    EveNgNodeInfo node = new EveNgNodeInfo(
                            nodeData.path("id").asText(),
                            nodeData.path("name").asText(),
                            nodeData.path("type").asText(),
                            nodeData.path("status").asInt(0),
                            nodeData.path("cpu").asInt(1),
                            nodeData.path("ram").asInt(256),
                            nodeData.path("nvram").asInt(0),
                            nodeData.path("disk").asInt(0),
                            nodeData.path("image").asText(),
                            nodeData.path("console").asText("telnet")
                    );
                    nodes.add(node);
                } catch (Exception ex) {
                    log.warn("Failed to parse node entry: {}", ex.getMessage());
                }
            });

            log.info("✓ Retrieved {} nodes for lab '{}'", nodes.size(), evengLabId);
            return nodes;
        } catch (WebClientResponseException ex) {
            log.warn("Failed to get nodes for lab '{}': HTTP {}", evengLabId, ex.getStatusCode());
            throw new EveNgIntegrationException(
                    "Failed to get lab nodes: HTTP " + ex.getStatusCode(), ex);
        }
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
    }
}
