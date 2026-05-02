package com.mnco.domain.repository;

import com.mnco.domain.entities.LabTemplate;
import com.mnco.domain.entities.LabTemplateStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for Lab Template persistence.
 */
public interface LabTemplateRepository {

    LabTemplate save(LabTemplate template);

    Optional<LabTemplate> findById(UUID id);

    List<LabTemplate> findAll();

    List<LabTemplate> findAllByStatus(LabTemplateStatus status);

    Optional<LabTemplate> findByEveTemplatePath(String path);

    Optional<LabTemplate> findByName(String name);

    void deleteById(UUID id);
}
