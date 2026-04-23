package com.mnco.domain.repository;

import com.mnco.domain.entities.LabAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting lab assignments (lab_assignments table).
 * Provides checks for tenant isolation and assignment existence.
 */
public interface LabAssignmentRepository {

    LabAssignment save(LabAssignment assignment);

    Optional<LabAssignment> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabAssignment> findByUserId(UUID userId);

    List<LabAssignment> findByTemplateId(UUID templateId);

    boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId);

    void deleteByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabAssignment> findByAssignedBy(UUID assignedBy);

    List<LabAssignment> findByTemplateIdAndAssignedBy(UUID templateId, UUID assignedBy);
}
