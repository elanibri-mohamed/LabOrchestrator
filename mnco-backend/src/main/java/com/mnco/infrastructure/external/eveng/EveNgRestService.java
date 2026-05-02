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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EveNgRestService implements EveNgService {

    private final WebClient webClient;
    private final String username;
    private final String password;
    private final String evengHost;
    private final boolean strictCloneMode;
    private final AtomicBoolean nodesExportUnsupportedLogged = new AtomicBoolean(false);

    public EveNgRestService(
            @Value("${eveng.base-url}") String baseUrl,
            @Value("${eveng.username}") String username,
            @Value("${eveng.password}") String password,
            @Value("${eveng.clone.strict-mode:false}") boolean strictCloneMode) {

        this.username = username;
        this.password = password;
        this.evengHost = baseUrl.replaceFirst("https?://", "");
        this.strictCloneMode = strictCloneMode;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String authenticate() {
        return authenticate(username, password);
    }

    private String authenticate(String username, String password) {
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
        startLab(evengLabId, username, password);
    }

    @Override
    public void startLab(String evengLabId, String username, String password) {
        log.info("Starting nodes in lab '{}'", evengLabId);
        String cookie = authenticate(username, password);
        try {
            String uri = "/api/labs/" + evengLabId.replaceFirst("^/", "") + "/nodes/start";
            webClient.get().uri(uri).header("Cookie", cookie).retrieve().bodyToMono(String.class).block();
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Start lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void stopLab(String evengLabId) {
        stopLab(evengLabId, username, password);
    }

    @Override
    public void stopLab(String evengLabId, String username, String password) {
        log.info("Stopping nodes in lab '{}'", evengLabId);
        String cookie = authenticate(username, password);
        try {
            String uri = "/api/labs/" + evengLabId.replaceFirst("^/", "") + "/nodes/stop";
            webClient.get().uri(uri).header("Cookie", cookie).retrieve().bodyToMono(String.class).block();
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Stop lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void createUser(String username, String password, String role) {
        log.info("Creating user '{}' in EVE-NG", username);
        String cookie = authenticate();
        try {
            int pod = computePod(username);
            // EVE-NG Community Edition only supports admin role.
            Map<String, Object> payload = Map.of(
                    "username", username,
                    "password", password,
                    "role", "admin",
                    "expiration", -1,
                    "can_spawn", 1,
                "html5", 1,
                "pod", pod
            );

            JsonNode updateResponse = webClient.put()
                    .uri(uriBuilder -> uriBuilder.path("/api/users/{u}").build(username))
                    .header("Cookie", cookie)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (updateResponse != null && updateResponse.path("code").asInt() == 200) {
                return;
            }

            JsonNode createResponse = webClient.post()
                    .uri("/api/users")
                    .header("Cookie", cookie)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            int code = createResponse == null ? 0 : createResponse.path("code").asInt();
            if (code != 201 && code != 200) {
                String message = createResponse == null ? "empty response" : createResponse.path("message").asText("unknown");
                throw new EveNgIntegrationException("Failed to sync EVE-NG user '" + username + "': code=" + code + ", message=" + message);
            }
        } catch (Exception ex) {
            log.warn("Failed to create/update user '{}' in EVE-NG: {}", username, ex.getMessage());
        }
    }

    private int computePod(String username) {
        CRC32 crc = new CRC32();
        crc.update(username.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long hash = crc.getValue();
        int minPod = 1024;
        int range = 60000;
        return minPod + (int) (hash % range);
    }

    @Override
    public void deleteLab(String evengLabId) {
        deleteLab(evengLabId, username, password);
    }

    @Override
    public void deleteLab(String evengLabId, String username, String password) {
        log.info("Deleting lab '{}'", evengLabId);
        String cookie = authenticate(username, password);
        try {
            webClient.delete().uri("/api/labs" + evengLabId).header("Cookie", cookie).retrieve().toBodilessEntity().block();
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Delete lab failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void copyLab(String sourcePath, String targetPath) {
        copyLab(sourcePath, targetPath, username, password);
    }

    @Override
    public void copyLab(String sourcePath, String targetPath, String username, String password) {
        log.info("Copying EVE-NG lab '{}' → '{}'", sourcePath, targetPath);
        String cookie = authenticate(username, password);

        try {
            boolean cloneCompleted = false;

            try {
                cloneCompleted = copyLabUsingCreateWithSource(sourcePath, targetPath, cookie);
            } catch (Exception sourceCloneError) {
                log.warn("Source-clone flow failed for '{}' -> '{}': {}", sourcePath, targetPath, sourceCloneError.getMessage());
                if (strictCloneMode) {
                    throw new EveNgIntegrationException("Source-clone flow failed and strict clone mode is enabled", sourceCloneError);
                }
            }

            if (cloneCompleted) {
                log.info("Lab copied successfully to {} using source-clone flow", targetPath);
                return;
            }

            try {
                cloneCompleted = copyLabUsingNativeClone(sourcePath, targetPath, cookie);
            } catch (Exception nativeCloneError) {
                log.warn("Native clone flow failed for '{}' -> '{}': {}", sourcePath, targetPath, nativeCloneError.getMessage());
                if (strictCloneMode) {
                    throw new EveNgIntegrationException("Native clone flow failed and strict clone mode is enabled", nativeCloneError);
                }
            }

            if (cloneCompleted) {
                log.info("Lab copied successfully to {} using native clone flow", targetPath);
                return;
            }

            if (strictCloneMode) {
                throw new EveNgIntegrationException("Native clone flow did not complete and strict clone mode is enabled");
            }

            log.warn("Falling back to legacy rebuild copy flow for '{}'", targetPath);

            // 1. Get source lab info
            JsonNode sourceLab = webClient.get()
                    .uri("/api/labs" + sourcePath)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            
            if (sourceLab == null || !sourceLab.has("data")) throw new EveNgIntegrationException("Source lab not found");
            JsonNode labData = sourceLab.get("data");

            // 2. Ensure target folder exists
            String folderPath = targetPath.substring(0, targetPath.lastIndexOf("/"));
            if (folderPath.isEmpty()) folderPath = "/";
            createFolderInternal(folderPath, cookie);

            // 3. Delete target lab if it already exists to prevent 412 Precondition Failed
            try {
                webClient.delete()
                        .uri("/api/labs" + targetPath)
                        .header("Cookie", cookie)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } catch (Exception ignored) {
                // Ignore if it doesn't exist
            }

            // 4. Create target lab
            String fileName = targetPath.substring(targetPath.lastIndexOf("/") + 1).replace(".unl", "");
            Map<String, Object> newLabPayload = Map.of(
                    "path", folderPath,
                    "name", fileName,
                    "version", labData.path("version").asText("1"),
                    "author", labData.path("author").asText(""),
                    "description", labData.path("description").asText(""),
                    "body", labData.path("body").asText("")
            );

            webClient.post()
                    .uri("/api/labs")
                    .header("Cookie", cookie)
                    .bodyValue(newLabPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            // 4. Copy Networks
            JsonNode networksResponse = webClient.get()
                    .uri("/api/labs" + sourcePath + "/networks")
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            
            if (networksResponse != null && networksResponse.has("data")) {
                networksResponse.get("data").fields().forEachRemaining(entry -> {
                    JsonNode net = entry.getValue();
                    Map<String, Object> netPayload = new HashMap<>();
                    net.fields().forEachRemaining(f -> {
                        if (!"id".equals(f.getKey())) {
                            netPayload.put(f.getKey(), jsonNodeToJava(f.getValue()));
                        }
                    });
                    
                    webClient.post()
                            .uri("/api/labs" + targetPath + "/networks")
                            .header("Cookie", cookie)
                            .bodyValue(netPayload)
                            .retrieve()
                            .toBodilessEntity()
                            .block();
                });
            }

            // 5. Copy Nodes
            JsonNode nodesResponse = webClient.get()
                    .uri("/api/labs" + sourcePath + "/nodes")
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            
            if (nodesResponse != null && nodesResponse.has("data")) {
                nodesResponse.get("data").fields().forEachRemaining(entry -> {
                    JsonNode node = entry.getValue();
                    Map<String, Object> nodePayload = new HashMap<>();
                    node.fields().forEachRemaining(f -> {
                        if (!"id".equals(f.getKey()) && !"url".equals(f.getKey()) && !"status".equals(f.getKey())) {
                            nodePayload.put(f.getKey(), jsonNodeToJava(f.getValue()));
                        }
                    });

                    webClient.post()
                            .uri("/api/labs" + targetPath + "/nodes")
                            .header("Cookie", cookie)
                            .bodyValue(nodePayload)
                            .retrieve()
                            .toBodilessEntity()
                            .block();
                });
            }

            // 6. Copy topology links so node-to-node and node-to-network connections are preserved.
            copyTopologyLinks(sourcePath, targetPath, cookie);

            // 7. Export nodes configs (saves to the .unl)
            exportNodeConfigsBestEffort(targetPath, cookie, "for cloned lab");

            log.info("Lab copied successfully to {}", targetPath);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("WebClient error during copyLab: {} - Body: {}", ex.getMessage(), responseBody);
            throw new EveNgIntegrationException("Copy lab failed: " + ex.getMessage() + " | Response: " + responseBody, ex);
        } catch (Exception ex) {
            throw new EveNgIntegrationException("Copy lab failed: " + ex.getMessage(), ex);
        }
    }

    private boolean copyLabUsingCreateWithSource(String sourcePath, String targetPath, String cookie) {
        String targetFolderPath = extractFolderPath(targetPath);
        String targetLabName = extractFileName(targetPath).replaceFirst("\\.unl$", "");
        String expectedPrefix = targetLabName + "-mnco-clone-";

        createFolderInternal(targetFolderPath, cookie);

        deleteLabIfExists(targetPath, cookie);

        for (int attempt = 1; attempt <= 3; attempt++) {
            String tempCloneName = targetLabName + "-mnco-clone-" + System.currentTimeMillis() + "-" + attempt;
            String tempClonePath = targetFolderPath + "/" + tempCloneName + ".unl";

            Map<String, Object> payload = Map.of(
                "path", targetFolderPath,
                "name", tempCloneName,
                "source", sourcePath
            );

            deleteLabIfExists(tempClonePath, cookie);
            Set<String> beforeClonePaths = getLabPathsInFolder(targetFolderPath, cookie);
            Set<String> beforeAllLabPaths = getAllLabPaths(cookie);

            try {
                JsonNode response = webClient.post()
                        .uri("/api/labs")
                        .header("Cookie", cookie)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                String status = response != null ? response.path("status").asText("") : "";
                String message = response != null ? response.path("message").asText("") : "";
                log.info("Source-clone API response status='{}' message='{}' on attempt {}", status, message, attempt);

                if (!"success".equalsIgnoreCase(status)) {
                    return false;
                }

                // EVE clone works more reliably with a temporary name; rename to final target afterwards.
                Set<String> afterClonePaths = getLabPathsInFolder(targetFolderPath, cookie);
                Set<String> afterAllLabPaths = getAllLabPaths(cookie);
                String actualClonePath = resolveClonedPathInTargetFolder(
                        beforeClonePaths,
                        afterClonePaths,
                    beforeAllLabPaths,
                    afterAllLabPaths,
                        tempClonePath,
                        targetPath,
                        targetFolderPath,
                    targetLabName,
                    expectedPrefix,
                    cookie
                );
                if (actualClonePath == null) {
                    throw new EveNgIntegrationException("Could not resolve cloned lab path in target folder");
                }

                deleteLabIfExists(targetPath, cookie);

                String actualFolderPath = extractFolderPath(actualClonePath);
                if (!targetFolderPath.equals(actualFolderPath)) {
                    webClient.put()
                        .uri("/api/labs" + actualClonePath + "/move")
                        .header("Cookie", cookie)
                        .bodyValue(Map.of("path", targetFolderPath))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
                    actualClonePath = targetFolderPath + "/" + extractFileName(actualClonePath);
                    log.info("Source-clone move completed to '{}'", actualClonePath);
                }

                if (!targetPath.equals(actualClonePath)) {
                    webClient.put()
                            .uri("/api/labs" + actualClonePath)
                            .header("Cookie", cookie)
                            .bodyValue(Map.of("name", targetLabName))
                            .retrieve()
                            .toBodilessEntity()
                            .block();
                    log.info("Source-clone rename completed from '{}' to '{}'", actualClonePath, targetPath);
                } else {
                    log.info("Source-clone produced target path directly: {}", targetPath);
                }

                exportNodeConfigsBestEffort(targetPath, cookie, "after source clone");

                return true;
            } catch (WebClientResponseException ex) {
                String body = ex.getResponseBodyAsString();
                log.warn(
                    "Source-clone flow failed on attempt {}: HTTP {} body='{}'",
                        attempt,
                        ex.getStatusCode(),
                        body
                );

                // EVE may briefly lock the file after stop/delete; retry with cleanup.
                if (attempt < 3) {
                    deleteLabIfExists(targetPath, cookie);
                    deleteLabIfExists(tempClonePath, cookie);
                    pauseMillis(300L * attempt);
                    continue;
                }
                return false;
            } catch (Exception ex) {
                log.warn("Source-clone flow failed on attempt {}: {}", attempt, ex.getMessage());
                if (attempt < 3) {
                    deleteLabIfExists(targetPath, cookie);
                    deleteLabIfExists(tempClonePath, cookie);
                    pauseMillis(300L * attempt);
                    continue;
                }
                return false;
            }
        }

        return false;
    }

    private String resolveClonedPathInTargetFolder(Set<String> beforeClonePaths,
                                                   Set<String> afterClonePaths,
                                                   Set<String> beforeAllLabPaths,
                                                   Set<String> afterAllLabPaths,
                                                   String preferredTempPath,
                                                   String targetPath,
                                                   String targetFolderPath,
                                                   String targetLabName,
                                                   String expectedPrefix,
                                                   String cookie) {
        if (labExists(targetPath, cookie)) {
            return targetPath;
        }

        if (labExists(preferredTempPath, cookie)) {
            return preferredTempPath;
        }

        Set<String> added = new HashSet<>(afterClonePaths != null ? afterClonePaths : Collections.emptySet());
        if (beforeClonePaths != null && !beforeClonePaths.isEmpty()) {
            added.removeAll(beforeClonePaths);
        }

        if (!added.isEmpty()) {
            for (String p : added) {
                if (!extractFolderPath(p).equals(targetFolderPath)) continue;
                String n = extractFileName(p).replaceFirst("\\.unl$", "");
                if (n.startsWith(expectedPrefix)) {
                    return p;
                }
            }
            for (String p : added) {
                if (extractFolderPath(p).equals(targetFolderPath)) {
                    return p;
                }
            }
        }

        Set<String> addedGlobal = new HashSet<>(afterAllLabPaths != null ? afterAllLabPaths : Collections.emptySet());
        if (beforeAllLabPaths != null && !beforeAllLabPaths.isEmpty()) {
            addedGlobal.removeAll(beforeAllLabPaths);
        }

        if (!addedGlobal.isEmpty()) {
            for (String p : addedGlobal) {
                String n = extractFileName(p).replaceFirst("\\.unl$", "");
                if (n.startsWith(expectedPrefix)) {
                    return p;
                }
            }
            return addedGlobal.iterator().next();
        }

        return null;
    }

    private Set<String> getAllLabPaths(String cookie) {
        Set<String> paths = new HashSet<>();
        collectLabPathsRecursively("/", cookie, paths);
        return paths;
    }

    private void collectLabPathsRecursively(String folderPath, String cookie, Set<String> paths) {
        try {
            String uri = "/api/folders" + folderPath;
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode data = response != null ? response.path("data") : null;
            JsonNode labs = data != null ? data.path("labs") : null;
            if (labs != null && labs.isArray()) {
                for (JsonNode lab : labs) {
                    String p = lab.path("path").asText(null);
                    if (p != null && p.endsWith(".unl")) {
                        paths.add(normalizeLabPath(p));
                    }
                }
            }

            JsonNode folders = data != null ? data.path("folders") : null;
            if (folders != null && folders.isArray()) {
                for (JsonNode f : folders) {
                    String name = f.path("name").asText("");
                    String p = f.path("path").asText(null);
                    if (p != null && !"..".equals(name)) {
                        collectLabPathsRecursively(normalizeLabPath(p), cookie, paths);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not recursively list folder '{}': {}", folderPath, ex.getMessage());
        }
    }

    private void deleteLabIfExists(String labPath, String cookie) {
        try {
            webClient.delete()
                    .uri("/api/labs" + labPath)
                    .header("Cookie", cookie)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ignored) {
            // Ignore when target does not exist or cannot be deleted.
        }
    }

    private void pauseMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void exportNodeConfigsBestEffort(String targetPath, String cookie, String context) {
        try {
            webClient.get()
                    .uri("/api/labs" + targetPath + "/nodes/export")
                    .header("Cookie", cookie)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 400) {
                if (nodesExportUnsupportedLogged.compareAndSet(false, true)) {
                    log.info("EVE-NG nodes export endpoint returned HTTP 400 (unsupported on this server); continuing without export.");
                } else {
                    log.debug("Skipping nodes export {} due to HTTP 400: {}", context, ex.getMessage());
                }
                return;
            }
            log.warn("Failed to export node configs {} (ignoring): HTTP {} body='{}'", context, ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("Failed to export node configs {} (ignoring): {}", context, ex.getMessage());
        }
    }

    private boolean copyLabUsingNativeClone(String sourcePath, String targetPath, String cookie) {
        log.info("Native clone flow started for '{}' -> '{}'", sourcePath, targetPath);

        String targetFolderPath = extractFolderPath(targetPath);
        String targetFileName = extractFileName(targetPath);
        String targetLabName = targetFileName.replaceFirst("\\.unl$", "");
        String sourceFolderPath = extractFolderPath(sourcePath);
        String sourceBaseName = extractFileName(sourcePath).replaceFirst("\\.unl$", "");

        createFolderInternal(targetFolderPath, cookie);

        Set<String> beforeClonePaths = getLabPathsInFolder(sourceFolderPath, cookie);
        log.info("Native clone flow source folder snapshot has {} lab(s)", beforeClonePaths.size());

        // Keep idempotent behavior: clear existing target before moving/renaming cloned file.
        try {
            webClient.delete()
                    .uri("/api/labs" + targetPath)
                    .header("Cookie", cookie)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ignored) {
            // Ignore if target does not exist.
        }

        JsonNode cloneResponse = invokeNativeClone(sourcePath, targetLabName, targetFolderPath, cookie);
        if (cloneResponse == null) {
            return false;
        }

        String clonedPath = extractClonedPath(cloneResponse, sourcePath);
        if ((clonedPath == null || clonedPath.isBlank()) && labExists(targetPath, cookie)) {
            clonedPath = targetPath;
            log.info("Native clone flow resolved cloned lab by existence check: {}", clonedPath);
        }

        if (clonedPath == null || clonedPath.isBlank()) {
            Set<String> afterClonePaths = getLabPathsInFolder(sourceFolderPath, cookie);
            log.info("Native clone flow source folder after-clone snapshot has {} lab(s)", afterClonePaths.size());
            clonedPath = detectClonedPath(beforeClonePaths, afterClonePaths, sourcePath, sourceBaseName);
        }

        if (clonedPath == null || clonedPath.isBlank()) {
            log.warn("Native clone response did not include cloned path for source '{}'", sourcePath);
            return false;
        }

        log.info("Native clone flow resolved cloned lab path: {}", clonedPath);

        String movedPath = clonedPath;
        String clonedFolder = extractFolderPath(clonedPath);
        if (!targetFolderPath.equals(clonedFolder)) {
            webClient.put()
                    .uri("/api/labs" + clonedPath + "/move")
                    .header("Cookie", cookie)
                    .bodyValue(Map.of("path", targetFolderPath))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            movedPath = targetFolderPath + "/" + extractFileName(clonedPath);
            log.info("Native clone flow moved lab to: {}", movedPath);
        } else {
            log.info("Native clone flow did not need move step for {}", movedPath);
        }

        String movedName = extractFileName(movedPath).replaceFirst("\\.unl$", "");
        if (!targetLabName.equals(movedName)) {
            webClient.put()
                    .uri("/api/labs" + movedPath)
                    .header("Cookie", cookie)
                    .bodyValue(Map.of("name", targetLabName))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Native clone flow renamed lab from '{}' to '{}'", movedName, targetLabName);
        } else {
            log.info("Native clone flow did not need rename step for {}", movedPath);
        }

        exportNodeConfigsBestEffort(targetPath, cookie, "after native clone");

        log.info("Native clone flow finished with final target path: {}", targetPath);

        return true;
    }

    private JsonNode invokeNativeClone(String sourcePath, String targetLabName, String targetFolderPath, String cookie) {
        String cloneUri = "/api/labs" + sourcePath + "/clone";

        // EVE APIs often use GET for actions (start/stop/wipe/export), and docs note some calls are outdated.
        // Try GET clone first to match common EVE behavior.
        try {
            JsonNode response = webClient.get()
                    .uri(cloneUri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String cloneMessage = response != null ? response.path("message").asText("") : "";
            log.info("Native clone API succeeded with GET. message='{}'", cloneMessage);
            return response;
        } catch (WebClientResponseException ex) {
            log.warn(
                    "Native clone GET failed: HTTP {} body='{}'",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            log.warn("Native clone GET failed: {}", ex.getMessage());
        }

        List<Map<String, Object>> payloadAttempts = List.of(
                Collections.emptyMap(),
                Map.of("name", targetLabName),
                Map.of("path", targetFolderPath),
                Map.of("name", targetLabName, "path", targetFolderPath)
        );

        for (Map<String, Object> payload : payloadAttempts) {
            try {
                JsonNode response = webClient.post()
                        .uri(cloneUri)
                        .header("Cookie", cookie)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                String cloneMessage = response != null ? response.path("message").asText("") : "";
                log.info("Native clone API succeeded with payload {}. message='{}'", payload, cloneMessage);
                return response;
            } catch (WebClientResponseException ex) {
                log.warn(
                        "Native clone POST failed with payload {}: HTTP {} body='{}'",
                        payload,
                        ex.getStatusCode(),
                        ex.getResponseBodyAsString()
                );
            } catch (Exception ex) {
                log.warn("Native clone POST failed with payload {}: {}", payload, ex.getMessage());
            }
        }

        try {
            JsonNode response = webClient.put()
                    .uri(cloneUri)
                    .header("Cookie", cookie)
                    .bodyValue(Map.of("name", targetLabName, "path", targetFolderPath))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String cloneMessage = response != null ? response.path("message").asText("") : "";
            log.info("Native clone API succeeded with PUT fallback. message='{}'", cloneMessage);
            return response;
        } catch (WebClientResponseException ex) {
            log.warn(
                    "Native clone PUT fallback failed: HTTP {} body='{}'",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            log.warn("Native clone PUT fallback failed: {}", ex.getMessage());
        }

        log.warn("Native clone endpoint unavailable for '{}' after trying multiple payload formats", sourcePath);
        return null;
    }

    private boolean labExists(String labPath, String cookie) {
        try {
            webClient.get()
                    .uri("/api/labs" + labPath)
                    .header("Cookie", cookie)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String extractClonedPath(JsonNode cloneResponse, String sourcePath) {
        if (cloneResponse == null) {
            return null;
        }

        JsonNode data = cloneResponse.path("data");
        if (data.isTextual() && data.asText().endsWith(".unl")) {
            return normalizeLabPath(data.asText());
        }

        if (data.isObject()) {
            String path = firstNonBlank(
                    data.path("path").asText(null),
                    data.path("labPath").asText(null),
                    data.path("lab_path").asText(null),
                    data.path("url").asText(null)
            );
            if (path != null && path.endsWith(".unl")) {
                return normalizeLabPath(path);
            }

            String name = firstNonBlank(data.path("name").asText(null), data.path("labName").asText(null));
            if (name != null) {
                String sourceFolder = extractFolderPath(sourcePath);
                String fileName = name.endsWith(".unl") ? name : name + ".unl";
                return sourceFolder + "/" + fileName;
            }
        }

        String message = cloneResponse.path("message").asText(null);
        if (message != null) {
            Matcher pathMatcher = Pattern.compile("(/[^\\s]+\\.unl)").matcher(message);
            if (pathMatcher.find()) {
                return normalizeLabPath(pathMatcher.group(1));
            }

            Matcher nameMatcher = Pattern.compile("'([^']+\\.unl)'|\"([^\"]+\\.unl)\"").matcher(message);
            if (nameMatcher.find()) {
                String matched = firstNonBlank(nameMatcher.group(1), nameMatcher.group(2));
                if (matched != null) {
                    return extractFolderPath(sourcePath) + "/" + matched;
                }
            }
        }

        return null;
    }

    private Set<String> getLabPathsInFolder(String folderPath, String cookie) {
        try {
            String uri = "/api/folders" + folderPath;
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Set<String> paths = new HashSet<>();
            JsonNode labs = response != null ? response.path("data").path("labs") : null;
            if (labs != null && labs.isArray()) {
                for (JsonNode lab : labs) {
                    String path = lab.path("path").asText(null);
                    if (path != null && path.endsWith(".unl")) {
                        paths.add(normalizeLabPath(path));
                    }
                }
            }
            return paths;
        } catch (Exception ex) {
            log.debug("Could not list folder '{}' before/after clone: {}", folderPath, ex.getMessage());
            return Collections.emptySet();
        }
    }

    private String detectClonedPath(Set<String> beforeClonePaths, Set<String> afterClonePaths, String sourcePath, String sourceBaseName) {
        if (afterClonePaths == null || afterClonePaths.isEmpty()) {
            return null;
        }

        Set<String> newPaths = new HashSet<>(afterClonePaths);
        if (beforeClonePaths != null && !beforeClonePaths.isEmpty()) {
            newPaths.removeAll(beforeClonePaths);
        }

        if (newPaths.isEmpty()) {
            return null;
        }

        String sourceNormalized = normalizeLabPath(sourcePath);
        String sourceFolder = extractFolderPath(sourceNormalized);

        for (String candidate : newPaths) {
            if (!extractFolderPath(candidate).equals(sourceFolder)) {
                continue;
            }
            String candidateName = extractFileName(candidate).replaceFirst("\\.unl$", "");
            if (candidateName.equals(sourceBaseName) || candidateName.startsWith(sourceBaseName)) {
                return candidate;
            }
        }

        return newPaths.iterator().next();
    }

    private String normalizeLabPath(String path) {
        if (path == null || path.isBlank()) return path;
        String noHost = path.replaceFirst("^https?://[^/]+", "");
        if (noHost.startsWith("/api/labs")) {
            return noHost.substring("/api/labs".length());
        }
        return noHost.startsWith("/") ? noHost : "/" + noHost;
    }

    private String extractFolderPath(String labPath) {
        int idx = labPath.lastIndexOf('/');
        if (idx <= 0) return "/";
        return labPath.substring(0, idx);
    }

    private String extractFileName(String labPath) {
        int idx = labPath.lastIndexOf('/');
        return idx >= 0 ? labPath.substring(idx + 1) : labPath;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private void copyTopologyLinks(String sourcePath, String targetPath, String cookie) {
        try {
            JsonNode topologyResponse = webClient.get()
                    .uri("/api/labs" + sourcePath + "/topology")
                    .header("Cookie", cookie)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (topologyResponse == null || !topologyResponse.has("data") || !topologyResponse.get("data").isArray()) {
                log.warn("Source topology payload missing for '{}'; skipping link clone", sourcePath);
                return;
            }

            JsonNode topologyPayload = topologyResponse.get("data");
            RuntimeException lastFailure = null;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    webClient.post()
                            .uri("/api/labs" + targetPath + "/topology")
                            .header("Cookie", cookie)
                            .bodyValue(topologyPayload)
                            .retrieve()
                            .toBodilessEntity()
                            .block();
                    return;
                } catch (Exception postEx) {
                    lastFailure = new RuntimeException(postEx);
                    try {
                        webClient.put()
                                .uri("/api/labs" + targetPath + "/topology")
                                .header("Cookie", cookie)
                                .bodyValue(topologyPayload)
                                .retrieve()
                                .toBodilessEntity()
                                .block();
                        return;
                    } catch (Exception putEx) {
                        lastFailure = new RuntimeException(putEx);
                        if (attempt < 3) {
                            try {
                                Thread.sleep(250L * attempt);
                            } catch (InterruptedException interruptedException) {
                                Thread.currentThread().interrupt();
                                throw new EveNgIntegrationException("Interrupted while cloning topology links", interruptedException);
                            }
                        }
                    }
                }
            }

            if (lastFailure != null) {
                throw lastFailure;
            }
        } catch (Exception ex) {
            log.warn("Failed to copy topology links from '{}' to '{}': {}", sourcePath, targetPath, ex.getMessage());
        }
    }

    private Object jsonNodeToJava(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        if (node.isInt() || node.isLong()) return node.asLong();
        if (node.isFloat() || node.isDouble() || node.isBigDecimal()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            node.forEach(item -> values.add(jsonNodeToJava(item)));
            return values;
        }
        if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), jsonNodeToJava(entry.getValue())));
            return map;
        }
        return node.asText();
    }

    @Override
    public void createFolder(String path) {
        createFolder(path, username, password);
    }

    @Override
    public void createFolder(String path, String username, String password) {
        createFolderInternal(path, authenticate(username, password));
    }

    private void createFolderInternal(String path, String cookie) {
        if (path == null || path.equals("/") || path.isEmpty()) return;
        
        log.info("Ensuring folder exists: {}", path);
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
        return getLabNodeStatuses(evengLabId, username, password);
    }

    @Override
    public List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId, String username, String password) {
        String cookie = authenticate(username, password);
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
        return getNodeConsoleInfo(evengLabId, nodeId, username, password);
    }

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId, String username, String password) {
        String cookie = authenticate(username, password);
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
        return getAllLabs(username, password);
    }

    @Override
    public List<EveNgLabInfo> getAllLabs(String username, String password) {
        String cookie = authenticate(username, password);
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
                    getLabByPath(labEntry.path("path").asText(), cookie).ifPresent(allLabs::add);
                }
            }
            if (data.has("folders") && data.get("folders").isArray()) {
                for (JsonNode f : data.get("folders")) {
                    if (!"..".equals(f.path("name").asText())) traverseFolders(f.path("path").asText(), cookie, allLabs);
                }
            }
        } catch (Exception ignored) {}
    }

    private Optional<EveNgLabInfo> getLabByPath(String path, String cookie) {
        try {
            JsonNode response = webClient.get().uri("/api/labs" + path).header("Cookie", cookie).retrieve().bodyToMono(JsonNode.class).block();
            if (response == null || !response.has("data")) return Optional.empty();
            JsonNode n = response.get("data");
            return Optional.of(new EveNgLabInfo(n.path("id").asText(), n.path("name").asText(), path, n.path("description").asText(), n.path("version").asText(), 0L, 0L, 0, 0));
        } catch (Exception ex) { return Optional.empty(); }
    }

    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId) {
        return getLabNodes(evengLabId, username, password);
    }

    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId, String username, String password) {
        String cookie = authenticate(username, password);
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
        return getRawLabNodes(evengLabId, username, password);
    }

    @Override
    public Map<String, Object> getRawLabNodes(String evengLabId, String username, String password) {
        String cookie = authenticate(username, password);
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
