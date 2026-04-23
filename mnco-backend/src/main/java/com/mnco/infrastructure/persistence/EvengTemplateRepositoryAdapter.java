package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.EvengTemplateMapper;
import com.mnco.domain.entities.EvengTemplate;
import com.mnco.domain.entities.LabTemplateStatus;
import com.mnco.domain.repository.EvengTemplateRepository;
import com.mnco.infrastructure.persistence.entity.EvengTemplateJpaEntity;
import com.mnco.infrastructure.persistence.repository.EvengTemplateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing EvengTemplateRepository using Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
public class EvengTemplateRepositoryAdapter implements EvengTemplateRepository {

    private final EvengTemplateJpaRepository jpaRepository;
    private final EvengTemplateMapper mapper;

    @Override
    public EvengTemplate save(EvengTemplate template) {
        EvengTemplateJpaEntity entity = mapper.toJpaEntity(template);
        EvengTemplateJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EvengTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<EvengTemplate> findByEvengTemplatePath(String evengTemplatePath) {
        return jpaRepository.findByEvengTemplatePath(evengTemplatePath).map(mapper::toDomain);
    }

    @Override
    public List<EvengTemplate> findAllActive() {
        return jpaRepository.findByTemplateStatus(LabTemplateStatus.ACTIVE).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EvengTemplate> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
