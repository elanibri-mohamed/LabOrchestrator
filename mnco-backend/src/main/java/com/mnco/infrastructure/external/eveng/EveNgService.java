package com.mnco.infrastructure.external.eveng;

import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port for all EVE-NG orchestration operations.
 */
public interface EveNgService {

    /** Start all nodes within a lab. */
    void startLab(String evengLabId);

    /** Start all nodes within a lab using explicit EVE-NG credentials. */
    void startLab(String evengLabId, String username, String password);

    /** Stop all nodes within a lab. */
    void stopLab(String evengLabId);

    /** Stop all nodes within a lab using explicit EVE-NG credentials. */
    void stopLab(String evengLabId, String username, String password);

    /** Permanently delete a lab topology from EVE-NG. */
    void deleteLab(String evengLabId);

    /** Permanently delete a lab topology from EVE-NG using explicit credentials. */
    void deleteLab(String evengLabId, String username, String password);

    /** 
     * Copy a template to a specific instance path.
     * @param sourcePath absolute EVE-NG path (e.g. /templates/lab1.unl)
     * @param targetPath absolute EVE-NG path (e.g. /instances/user1/lab1.unl)
     */
    void copyLab(String sourcePath, String targetPath);

    /** Copy a template to a specific instance path using explicit credentials. */
    void copyLab(String sourcePath, String targetPath, String username, String password);

    /** Create a folder if it doesn't exist. */
    void createFolder(String path);

    /** Create a folder if it doesn't exist using explicit credentials. */
    void createFolder(String path, String username, String password);

    /** Create a user in EVE-NG. */
    void createUser(String username, String password, String role);

    /** Retrieve status of all nodes in a lab. */
    List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId);

    /** Retrieve status of all nodes in a lab using explicit credentials. */
    List<EveNgNodeStatus> getLabNodeStatuses(String evengLabId, String username, String password);

    /** Get console connection details for a single node. */
    EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId);

    /** Get console connection details for a single node using explicit credentials. */
    EveNgNodeConsoleInfo getNodeConsoleInfo(String evengLabId, String nodeId, String username, String password);

    /** Retrieve all labs available in EVE-NG server. */
    List<EveNgLabInfo> getAllLabs();

    /** Retrieve all labs available in EVE-NG server using explicit credentials. */
    List<EveNgLabInfo> getAllLabs(String username, String password);

    /** Retrieve all nodes within a lab. */
    List<EveNgNodeInfo> getLabNodes(String evengLabId);

    /** Retrieve all nodes within a lab using explicit credentials. */
    List<EveNgNodeInfo> getLabNodes(String evengLabId, String username, String password);

    /** Retrieve all nodes within a lab with raw details (including URLs). */
    Map<String, Object> getRawLabNodes(String evengLabId);

    /** Retrieve all nodes within a lab with raw details using explicit credentials. */
    Map<String, Object> getRawLabNodes(String evengLabId, String username, String password);
}
