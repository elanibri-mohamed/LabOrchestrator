package com.mnco.domain.repository;

import com.mnco.domain.entities.EvengTemplate;
import com.mnco.domain.entities.LabTemplateStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying EVE-NG templates (eveng_templates table).
 */
public interface EvengTemplateRepository {

    EvengTemplate save(EvengTemplate template);

    Optional<EvengTemplate> findById(UUID id);

    Optional<EvengTemplate> findByEvengTemplatePath(String evengTemplatePath);

    List<EvengTemplate> findAllActive();

    List<EvengTemplate> findAll();

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
