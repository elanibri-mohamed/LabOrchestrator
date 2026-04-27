package com.mnco.application.usecases;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Port for Lab Instance management (start, stop, reset, console).
 */
public interface LabInstanceUseCase {

    LabResponse startInstance(UUID templateId, UUID userId);

    LabResponse stopInstance(UUID templateId, UUID userId);

    LabResponse resetInstance(UUID templateId, UUID userId);

    LabResponse resetTimer(UUID templateId, UUID userId);

    List<LabResponse> getMyInstances(UUID userId);

    EveNgNodeConsoleInfo getNodeConsoleInfo(UUID templateId, String nodeId, UUID userId);

    Map<String, Object> getInstanceNodes(UUID templateId, UUID userId);
}
