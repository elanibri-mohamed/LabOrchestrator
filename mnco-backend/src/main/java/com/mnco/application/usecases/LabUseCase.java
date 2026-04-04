package com.mnco.application.usecases;

import com.mnco.application.dto.request.CloneLabRequest;
import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;

import java.util.List;
import java.util.UUID;

/**
 * Port for all lab lifecycle use cases.
 */
public interface LabUseCase {

    LabResponse createLab(CreateLabRequest request, UUID ownerId);

    LabResponse startLab(UUID labId, UUID requesterId);

    LabResponse stopLab(UUID labId, UUID requesterId);

    void deleteLab(UUID labId, UUID requesterId);

    /** Clone a STOPPED lab into a new independent lab (FR-LM-06). */
    LabResponse cloneLab(UUID sourceLabId, CloneLabRequest request, UUID requesterId);

    List<LabResponse> getLabsByOwner(UUID ownerId);

    LabResponse getLabById(UUID labId, UUID requesterId, boolean isAdmin);

    List<LabResponse> getAllLabs();

    /** Get console connection details for a specific node (FR-LM-09). */
    EveNgNodeConsoleInfo getNodeConsoleInfo(UUID labId, String nodeId, UUID requesterId);
}
