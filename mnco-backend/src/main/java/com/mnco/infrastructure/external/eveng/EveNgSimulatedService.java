package com.mnco.infrastructure.external.eveng;

import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simulated EVE-NG implementation for local development and CI.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "eveng.simulation-mode", havingValue = "true")
public class EveNgSimulatedService implements EveNgService {

    @Override
    public void startLab(String evengLabId) {
        log.info("[SIM] Starting lab '{}'", evengLabId);
        sleep(1000);
    }

    @Override
    public void startLab(String evengLabId, String username, String password) {
        startLab(evengLabId);
    }

    @Override
    public void stopLab(String evengLabId) {
        log.info("[SIM] Stopping lab '{}'", evengLabId);
        sleep(500);
    }

    @Override
    public void stopLab(String evengLabId, String username, String password) {
        stopLab(evengLabId);
    }

    @Override
    public void deleteLab(String evengLabId) {
        log.info("[SIM] Deleting lab '{}'", evengLabId);
        sleep(400);
    }

    @Override
    public void deleteLab(String evengLabId, String username, String password) {
        deleteLab(evengLabId);
    }

    @Override
    public void copyLab(String sourcePath, String targetPath) {
        log.info("[SIM] Copying lab '{}' -> '{}'", sourcePath, targetPath);
        sleep(800);
    }

    @Override
    public void copyLab(String sourcePath, String targetPath, String username, String password) {
        copyLab(sourcePath, targetPath);
    }

    @Override
    public void createFolder(String path) {
        log.info("[SIM] Creating folder: {}", path);
    }

    @Override
    public void createFolder(String path, String username, String password) {
        createFolder(path);
    }

    @Override
    public void createUser(String username, String password, String role) {
        log.info("[SIM] Creating user '{}' in EVE-NG (role: {})", username, role);
    }

    @Override
    public List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId) {
        return List.of(
                new EveNgNodeStatus("1", "Router-1", 2, "iol", 1, 256),
                new EveNgNodeStatus("2", "PC-1", 0, "vpcs", 0, 64)
        );
    }

    @Override
    public List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId, String username, String password) {
        return getLabNodeStatuses(evengLabId);
    }

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId) {
        return new EveNgNodeConsoleInfo(
                "TELNET", "127.0.0.1", 32768, "ws://127.0.0.1/console",
                nodeId, "Node-" + nodeId, "RUNNING"
        );
    }

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId, String username, String password) {
        return getNodeConsoleInfo(evengLabId, nodeId);
    }

    @Override
    public List<EveNgLabInfo> getAllLabs() {
        long now = System.currentTimeMillis() / 1000;
        return List.of(
                new EveNgLabInfo("1", "Topology-1", "/Topology-1.unl", "Desc", "1.0", now, now, 0, 2)
        );
    }

    @Override
    public List<EveNgLabInfo> getAllLabs(String username, String password) {
        return getAllLabs();
    }


    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId) {
        return List.of(
                new EveNgNodeInfo("1", "Router-1", "iol", 2, 2, 2048, 512, 0, "img", "telnet")
        );
    }

    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId, String username, String password) {
        return getLabNodes(evengLabId);
    }

    @Override
    public java.util.Map<String, Object> getRawLabNodes(String evengLabId) {
        java.util.Map<String, Object> nodes = new java.util.HashMap<>();
        java.util.Map<String, Object> node1 = new java.util.HashMap<>();
        node1.put("id", 1);
        node1.put("name", "Router-1");
        node1.put("status", 2);
        node1.put("url", "http://127.0.0.1/html5/#/client/SIM1");
        nodes.put("1", node1);
        return nodes;
    }

    @Override
    public java.util.Map<String, Object> getRawLabNodes(String evengLabId, String username, String password) {
        return getRawLabNodes(evengLabId);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
