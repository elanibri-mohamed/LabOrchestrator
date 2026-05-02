package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.LabTemplateMapper;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.domain.entities.LabTemplateStatus;
import com.mnco.domain.repository.LabTemplateRepository;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import com.mnco.infrastructure.persistence.repository.LabTemplateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LabTemplateRepositoryAdapter implements LabTemplateRepository {

    private final LabTemplateJpaRepository jpaRepository;
    private final LabTemplateMapper mapper;

    @Override
    public LabTemplate save(LabTemplate template) {
        LabTemplateJpaEntity entity = mapper.toJpaEntity(template);
        LabTemplateJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<LabTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<LabTemplate> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LabTemplate> findAllByStatus(LabTemplateStatus status) {
        return jpaRepository.findAllByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<LabTemplate> findByEveTemplatePath(String path) {
        return jpaRepository.findByEveTemplatePath(path).map(mapper::toDomain);
    }

    @Override
    public Optional<LabTemplate> findByName(String name) {
        return jpaRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
