package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.LabAssignmentMapper;
import com.mnco.domain.entities.LabAssignment;
import com.mnco.domain.repository.LabAssignmentRepository;
import com.mnco.infrastructure.persistence.entity.LabAssignmentJpaEntity;
import com.mnco.infrastructure.persistence.repository.LabAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LabAssignmentRepositoryAdapter implements LabAssignmentRepository {

    private final LabAssignmentJpaRepository jpaRepository;
    private final LabAssignmentMapper mapper;

    @Override
    public LabAssignment save(LabAssignment assignment) {
        LabAssignmentJpaEntity entity = mapper.toJpaEntity(assignment);
        LabAssignmentJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<LabAssignment> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<LabAssignment> findByTemplateIdAndUserId(UUID templateId, UUID userId) {
        return jpaRepository.findByTemplateIdAndUserId(templateId, userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId) {
        return jpaRepository.existsByTemplateIdAndUserId(templateId, userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
