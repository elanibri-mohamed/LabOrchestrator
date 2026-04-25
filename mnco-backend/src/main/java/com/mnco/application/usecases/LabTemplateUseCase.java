package com.mnco.application.usecases;

import com.mnco.application.dto.response.LabResponse;
import java.util.List;
import java.util.UUID;

/**
 * Port for Lab Template management (sync, list).
 */
public interface LabTemplateUseCase {

    List<LabResponse> getAllTemplates();

    LabResponse getTemplateById(UUID id);

    /**
     * Discover and sync labs from EVE-NG server templates directory.
     * @return list of synced templates
     */
    List<LabResponse> discoverTemplatesFromEveNg();
}
