package com.mnco.domain.repository;

import com.mnco.domain.entities.LabInstance;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for Lab Instance persistence.
 */
public interface LabInstanceRepository {

    LabInstance save(LabInstance instance);

    Optional<LabInstance> findById(UUID id);

    Optional<LabInstance> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    Optional<LabInstance> findByEveInstancePath(String path);

    void deleteById(UUID id);

    java.util.List<LabInstance> findRunningLabsIdleSince(java.time.Instant threshold);
}
