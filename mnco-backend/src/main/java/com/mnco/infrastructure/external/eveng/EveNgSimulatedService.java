package com.mnco.infrastructure.external.eveng;

import com.mnco.domain.entities.Lab;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
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

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
    }
}
