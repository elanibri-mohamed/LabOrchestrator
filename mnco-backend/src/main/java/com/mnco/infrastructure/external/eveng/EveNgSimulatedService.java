package com.mnco.infrastructure.external.eveng;

import com.mnco.domain.entities.Lab;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Simulated EVE-NG implementation for local development and CI.
 * Activated when eveng.simulation-mode=true.
 * Implements all EveNgService methods including clone (FR-LM-06) and console (FR-LM-09).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "eveng.simulation-mode", havingValue = "true")
public class EveNgSimulatedService implements EveNgService {

    @Override
    public EveNgLabResult createTopology(Lab lab) {
        log.info("[SIM] Creating topology for lab '{}' id={}", lab.getName(), lab.getId());
        sleep(800);
        String labId = "/" + sanitize(lab.getName()) + "-" + lab.getId() + ".unl";
        String nodeId = "node-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SIM] Topology created: evengLabId='{}'", labId);
        return new EveNgLabResult(labId, nodeId, "/opt/unetlab/labs" + labId, "created");
    }

    @Override
    public void startLab(String evengLabId) {
        log.info("[SIM] Starting lab '{}'", evengLabId);
        sleep(1200);
        log.info("[SIM] Lab '{}' started", evengLabId);
    }

    @Override
    public void stopLab(String evengLabId) {
        log.info("[SIM] Stopping lab '{}'", evengLabId);
        sleep(600);
        log.info("[SIM] Lab '{}' stopped", evengLabId);
    }

    @Override
    public void deleteLab(String evengLabId) {
        log.info("[SIM] Deleting lab '{}'", evengLabId);
        sleep(400);
        log.info("[SIM] Lab '{}' deleted", evengLabId);
    }

    @Override
    public EveNgCloneResult cloneLab(String sourceEvengLabId, String cloneName, String cloneId) {
        log.info("[SIM] Cloning '{}' → name='{}' id={}", sourceEvengLabId, cloneName, cloneId);
        sleep(1000);
        String clonedPath = "/" + sanitize(cloneName) + "-" + cloneId + ".unl";
        log.info("[SIM] Clone created at '{}'", clonedPath);
        return new EveNgCloneResult(clonedPath, "/opt/unetlab/labs" + clonedPath, "cloned");
    }

    @Override
    public List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId) {
        log.debug("[SIM] Fetching node statuses for lab '{}'", evengLabId);
        return List.of(
                new EveNgNodeStatus("1", "Router-1", 2, "iol", 1, 256),
                new EveNgNodeStatus("2", "Switch-1", 2, "iol", 1, 256),
                new EveNgNodeStatus("3", "PC-1",     0, "vpcs", 0, 64)
        );
    }

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId) {
        log.debug("[SIM] Console info for node='{}' lab='{}'", nodeId, evengLabId);
        int port = 32768 + (int)(Math.random() * 1000);
        return new EveNgNodeConsoleInfo(
                "TELNET",
                "192.168.1.100",
                port,
                "ws://192.168.1.100:8080/console/" + nodeId,
                nodeId,
                "SimNode-" + nodeId,
                "RUNNING"
        );
    }

    // ── Lab Discovery ────────────────────────────────────────────────────────

    @Override
    public List<EveNgLabInfo> getAllLabs() {
        log.info("[SIM] Fetching all labs from simulated EVE-NG");
        long now = System.currentTimeMillis() / 1000;
        return List.of(
                new EveNgLabInfo("1", "NetworkTopology-1", "/NetworkTopology-1.unl",
                        "Basic networking lab with routers and switches", "1.0",
                        now - 86400, now - 3600, 0, 3),
                new EveNgLabInfo("2", "SecurityLab-1", "/SecurityLab-1.unl",
                        "Firewall and IDS configuration lab", "1.0",
                        now - 172800, now - 7200, 0, 5),
                new EveNgLabInfo("3", "CloudNetwork-1", "/CloudNetwork-1.unl",
                        "Multi-cloud networking simulation", "1.0",
                        now - 259200, now - 10800, 0, 4)
        );
    }

    @Override
    public Optional<EveNgLabInfo> getLabByPath(String evengLabPath) {
        log.debug("[SIM] Fetching lab by path: '{}'", evengLabPath);
        sleep(300);
        // Return the first matching lab from the simulated list
        return getAllLabs().stream()
                .filter(lab -> lab.path().equals(evengLabPath))
                .findFirst();
    }

    @Override
    public List<EveNgNodeInfo> getLabNodes(String evengLabId) {
        log.debug("[SIM] Fetching nodes for lab: '{}'", evengLabId);
        sleep(500);
        return List.of(
                new EveNgNodeInfo("1", "Router-1", "iol", 2, 2, 2048, 512, 0,
                        "i86bi_linux_l2-adventerprisek9-M", "telnet"),
                new EveNgNodeInfo("2", "Router-2", "iol", 2, 2, 2048, 512, 0,
                        "i86bi_linux_l2-adventerprisek9-M", "telnet"),
                new EveNgNodeInfo("3", "Switch-1", "iol", 0, 1, 1024, 256, 0,
                        "i86bi_linux_l2-adventerprisek9-M", "telnet"),
                new EveNgNodeInfo("4", "PC-1", "vpcs", 0, 0, 256, 0, 0,
                        "vpcs-8.2", "telnet"),
                new EveNgNodeInfo("5", "Docker-Web", "docker", 2, 4, 4096, 1024, 20,
                        "nginx:latest", "telnet")
        );
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
    }
}
