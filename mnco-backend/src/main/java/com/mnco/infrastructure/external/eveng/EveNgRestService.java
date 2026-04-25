package com.mnco.infrastructure.external.eveng;

import com.fasterxml.jackson.databind.JsonNode;
import com.mnco.exception.custom.EveNgIntegrationException;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
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

@Slf4j
@Service
public class EveNgRestService implements EveNgService {

    private final WebClient webClient;
    private final String username;
    private final String password;
    private final String evengHost;

    public EveNgRestService(
            @Value("${eveng.base-url}") String baseUrl,
            @Value("${eveng.username}") String username,
            @Value("${eveng.password}") String password) {

        this.username = username;
        this.password = password;
        this.evengHost = baseUrl.replaceFirst("https?://", "");

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String authenticate() {
        try {
            var response = webClient.post()
                    .uri("/api/auth/login")
                    .bodyValue(Map.of("username", username, "password", password, "html5", 1))
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

    @Override
    public void startLab(String evengLabId) {
        log.info("Starting nodes in lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            String uri = "/api/labs/" + evengLabId.replaceFirst("^/", "") + "/nodes/start";
            webClient.get().uri(uri).header("Cookie", cookie).retrieve().bodyToMono(String.class).block();
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Start lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void stopLab(String evengLabId) {
        log.info("Stopping nodes in lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            String uri = "/api/labs/" + evengLabId.replaceFirst("^/", "") + "/nodes/stop";
            webClient.get().uri(uri).header("Cookie", cookie).retrieve().bodyToMono(String.class).block();
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Stop lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void createUser(String username, String password, String role) {
        log.info("Creating user '{}' in EVE-NG with role '{}'", username, role);
        String cookie = authenticate();
        try {
            // EVE-NG roles: admin, user
            String evengRole = "admin".equalsIgnoreCase(role) ? "admin" : "user";
            
            webClient.post()
                    .uri("/api/users")
                    .header("Cookie", cookie)
                    .bodyValue(Map.of(
                            "username", username,
                            "password", password,
                            "role", evengRole,
                            "expiration", -1,
                            "can_spawn", 1,
                            "html5", 1
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .onErrorResume(e -> {
                        log.debug("User {} might already exist in EVE-NG: {}", username, e.getMessage());
                        return reactor.core.publisher.Mono.empty();
                    })
                    .block();
        } catch (Exception ex) {
            log.warn("Failed to create user in EVE-NG: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteLab(String evengLabId) {
        log.info("Deleting lab '{}'", evengLabId);
        String cookie = authenticate();
        try {
            webClient.delete().uri("/api/labs" + evengLabId).header("Cookie", cookie).retrieve().toBodilessEntity().block();
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Delete lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void copyLab(String sourcePath, String targetPath) {
        log.info("Copying EVE-NG lab '{}' → '{}'", sourcePath, targetPath);
        String cookie = authenticate();

        try {
            // 1. Export source
            byte[] exportedLab = webClient.get()
                    .uri("/api/labs{id}/export", sourcePath)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (exportedLab == null) throw new EveNgIntegrationException("Export failed");

            // 2. Ensure target folder exists
            String folderPath = targetPath.substring(0, targetPath.lastIndexOf("/"));
            createFolder(folderPath);

            // 3. Import to target
            // In EVE-NG, import usually goes to the "root" of the API call or a specific path if specified
            // We'll try to import and then RENAME if it doesn't support direct path import.
            // Actually, EVE-NG import API takes the path in the JSON payload sometimes.
            
            String fileName = targetPath.substring(targetPath.lastIndexOf("/") + 1).replace(".unl", "");
            
            webClient.post()
                    .uri("/api/labs/import")
                    .header("Cookie", cookie)
                    .bodyValue(Map.of("path", folderPath, "data", exportedLab, "name", fileName))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
                    
            log.info("Lab copied successfully to {}", targetPath);
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Copy lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void createFolder(String path) {
        if (path == null || path.equals("/") || path.isEmpty()) return;
        
        log.info("Ensuring folder exists: {}", path);
        String cookie = authenticate();
        try {
            // Split path into parts and create recursively
            String[] parts = path.replaceFirst("^/", "").split("/");
            String current = "";
            for (String part : parts) {
                String parent = current.isEmpty() ? "/" : current;
                current += "/" + part;

                String finalCurrent = current;
                webClient.post()
                        .uri("/api/folders")
                        .header("Cookie", cookie)
                        .bodyValue(Map.of("path", parent, "name", part))
                        .retrieve()
                        .toBodilessEntity()
                        .onErrorResume(e -> {
                            log.debug("Folder {} might already exist: {}", finalCurrent, e.getMessage());
                            return reactor.core.publisher.Mono.empty();
                        })
                        .block();
            }
        } catch (Exception ex) {
            log.warn("Create folder might have failed (often because it exists): {}", ex.getMessage());
        }
    }

    @Override
    public List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId) {
        String cookie = authenticate();
        try {
            JsonNode response = webClient.get().uri("/api/labs" + evengLabId + "/nodes").header("Cookie", cookie).retrieve().bodyToMono(JsonNode.class).block();
            if (response == null || !response.has("data")) return Collections.emptyList();
            List<EveNgNodeStatus> statuses = new ArrayList<>();
            response.get("data").fields().forEachRemaining(entry -> {
                JsonNode n = entry.getValue();
                statuses.add(new EveNgNodeStatus(n.path("id").asText(), n.path("name").asText(), n.path("status").asInt(), n.path("type").asText(), n.path("cpu").asInt(), n.path("ram").asInt()));
            });
            return statuses;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId) {
        String cookie = authenticate();
        try {
            JsonNode response = webClient.get().uri("/api/labs" + evengLabId + "/nodes/" + nodeId).header("Cookie", cookie).retrieve().bodyToMono(JsonNode.class).block();
            JsonNode data = response.get("data");
            return new EveNgNodeConsoleInfo(data.path("type").asText().contains("qemu") ? "VNC" : "TELNET", evengHost, data.path("console").asInt(0), null, nodeId, data.path("name").asText(), data.path("status").asInt() == 2 ? "RUNNING" : "STOPPED");
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Console info failed", ex);
        }
    }

    @Override
    public List<EveNgLabInfo> getAllLabs() {
        String cookie = authenticate();
        List<EveNgLabInfo> allLabs = new ArrayList<>();
        traverseFolders("/", cookie, allLabs);
        return allLabs;
    }

    private void traverseFolders(String folderPath, String cookie, List<EveNgLabInfo> allLabs) {
        try {
            String uri = folderPath.equals("/") ? "/api/folders/" : "/api/folders" + folderPath;
            JsonNode response = webClient.get().uri(uri).header("Cookie", cookie).retrieve().bodyToMono(JsonNode.class).block();
            if (response == null || !response.has("data")) return;
            JsonNode data = response.get("data");
            if (data.has("labs") && data.get("labs").isArray()) {
                for (JsonNode labEntry : data.get("labs")) {
                    getLabByPath(labEntry.path("path").asText()).ifPresent(allLabs::add);
                }
            }
            if (data.has("folders") && data.get("folders").isArray()) {
                for (JsonNode f : data.get("folders")) {
                    if (!"..".equals(f.path("name").asText())) traverseFolders(f.path("path").asText(), cookie, allLabs);
                }
            }
        } catch (Exception ignored) {}
    }

    private Optional<EveNgLabInfo> getLabByPath(String path) {
        String cookie = authenticate();
        try {
            JsonNode response = webClient.get().uri("/api/labs" + path).header("Cookie", cookie).retrieve().bodyToMono(JsonNode.class).block();
            if (response == null || !response.has("data")) return Optional.empty();
            JsonNode n = response.get("data");
            return Optional.of(new EveNgLabInfo(n.path("id").asText(), n.path("name").asText(), path, n.path("description").asText(), n.path("version").asText(), 0L, 0L, 0, 0));
        } catch (Exception ex) { return Optional.empty(); }
    }

    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId) {
        String cookie = authenticate();
        try {
            JsonNode response = webClient.get().uri("/api/labs" + evengLabId + "/nodes").header("Cookie", cookie).retrieve().bodyToMono(JsonNode.class).block();
            List<EveNgNodeInfo> nodes = new ArrayList<>();
            response.get("data").fields().forEachRemaining(entry -> {
                JsonNode n = entry.getValue();
                nodes.add(new EveNgNodeInfo(n.path("id").asText(), n.path("name").asText(), n.path("type").asText(), n.path("status").asInt(), n.path("cpu").asInt(1), n.path("ram").asInt(256), 0, 0, n.path("image").asText(), n.path("console").asText()));
            });
            return nodes;
        } catch (Exception ex) { return Collections.emptyList(); }
    }

    @Override
    public Map<String, Object> getRawLabNodes(String evengLabId) {
        String cookie = authenticate();
        try {
            Map<String, Object> raw = webClient.get().uri("/api/labs" + evengLabId + "/nodes").header("Cookie", cookie).retrieve().bodyToMono(Map.class).block();
            Map<String, Object> nodes = (Map<String, Object>) raw.get("data");
            nodes.forEach((id, nodeObj) -> {
                if (nodeObj instanceof Map) {
                    Map<String, Object> node = (Map<String, Object>) nodeObj;
                    String rel = (String) node.get("url");
                    if (rel != null && rel.startsWith("/")) node.put("url", "http://" + evengHost + rel);
                }
            });
            return nodes;
        } catch (Exception ex) { throw new EveNgIntegrationException("Raw nodes failed", ex); }
    }
}
