package com.mnco.infrastructure.external.eveng;

import com.mnco.domain.entities.Lab;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;

import java.util.List;
import java.util.Optional;

/**
 * Port for all EVE-NG orchestration operations.
 * Two implementations:
 *   - EveNgRestService      — real HTTP calls to EVE-NG API v2 (production)
 *   - EveNgSimulatedService — in-memory simulation (dev / test)
 */
public interface EveNgService {

    /** Create a new lab topology in EVE-NG. */
    EveNgLabResult createTopology(Lab lab);

    /** Start all nodes within a lab. */
    void startLab(String evengLabId);

    /** Stop all nodes within a lab. */
    void stopLab(String evengLabId);

    /** Permanently delete a lab topology from EVE-NG. */
    void deleteLab(String evengLabId);

    /**
     * Deep-copy a lab topology to a new path (FR-LM-06).
     * The clone is fully independent — modifying the original does not affect the clone.
     *
     * @param sourceEvengLabId  EVE-NG path of the source lab
     * @param cloneName         sanitized name for the new topology file
     * @param cloneId           platform UUID to embed in the new path (uniqueness)
     */
    EveNgCloneResult cloneLab(String sourceEvengLabId, String cloneName, String cloneId);

    /** Retrieve status of all nodes in a lab. */
    List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId);

    /**
     * Get console connection details for a single node (FR-LM-09).
     *
     * @param evengLabId  EVE-NG lab path
     * @param nodeId      node identifier within the lab
     * @return console details (protocol, host, port, webSocketUrl)
     */
    EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId);

    /**
     * Retrieve all labs available in EVE-NG server.
     * Used for lab discovery and synchronization (read-only mode).
     *
     * @return list of all labs found in EVE-NG
     */
    List<EveNgLabInfo> getAllLabs();

    /**
     * Retrieve details for a specific lab by its EVE-NG path.
     *
     * @param evengLabPath EVE-NG lab path (e.g., "/lab-name.unl")
     * @return lab details if found
     */
    Optional<EveNgLabInfo> getLabByPath(String evengLabPath);

    /**
     * Retrieve all nodes within a lab.
     * Used to parse resource specifications from node configurations.
     *
     * @param evengLabId EVE-NG lab path
     * @return list of nodes in the lab
     */
    List<EveNgNodeInfo> getLabNodes(String evengLabId);
}
