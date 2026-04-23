package com.mnco.domain.repository;

import com.mnco.domain.entities.LabInstance;
import com.mnco.domain.entities.InstanceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting lab instances (lab_instances table).
 * Manages the lifecycle of per-user private copies of templates.
 */
public interface LabInstanceRepository {

    LabInstance save(LabInstance instance);

    Optional<LabInstance> findById(UUID id);

    /**
     * Find instance for a specific user and template.
     * There is at most one instance per (template, user) due to UNIQUE constraint.
     */
    Optional<LabInstance> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabInstance> findByUserId(UUID userId);

    List<LabInstance> findByTemplateId(UUID templateId);

    List<LabInstance> findByStatus(InstanceStatus status);

    boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId);

    void deleteByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabInstance> findByUserIdAndStatus(UUID userId, InstanceStatus status);
}
