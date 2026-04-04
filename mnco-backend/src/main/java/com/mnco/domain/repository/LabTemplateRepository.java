package com.mnco.domain.repository;

import com.mnco.domain.entities.LabTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for LabTemplate persistence.
 */
public interface LabTemplateRepository {

    LabTemplate save(LabTemplate template);

    Optional<LabTemplate> findById(UUID id);

    List<LabTemplate> findAllPublic();

    List<LabTemplate> findByAuthorId(UUID authorId);

    boolean existsByName(String name);

    void deleteById(UUID id);
}
