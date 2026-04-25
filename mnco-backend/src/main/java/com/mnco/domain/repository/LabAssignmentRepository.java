package com.mnco.domain.repository;

import com.mnco.domain.entities.LabAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for Lab Assignment persistence.
 */
public interface LabAssignmentRepository {

    LabAssignment save(LabAssignment assignment);

    List<LabAssignment> findAllByUserId(UUID userId);

    Optional<LabAssignment> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId);

    void deleteById(UUID id);
}
