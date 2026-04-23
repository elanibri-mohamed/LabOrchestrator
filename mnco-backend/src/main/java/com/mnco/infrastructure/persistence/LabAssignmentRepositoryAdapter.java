package com.mnco.infrastructure.persistence;

import com.mnco.domain.entities.LabAssignment;
import com.mnco.domain.repository.LabAssignmentRepository;
import com.mnco.infrastructure.persistence.entity.LabAssignmentJpaEntity;
import com.mnco.infrastructure.persistence.repository.LabAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter for LabAssignmentRepository using JPA.
 */
@Component
@RequiredArgsConstructor
public class LabAssignmentRepositoryAdapter implements LabAssignmentRepository {

    private final LabAssignmentJpaRepository jpaRepository;

    @Override
    public LabAssignment save(LabAssignment assignment) {
        LabAssignmentJpaEntity entity = new LabAssignmentJpaEntity();
        entity.setId(assignment.getId());
        entity.setTemplateId(assignment.getTemplateId());
        entity.setUserId(assignment.getUserId());
        entity.setAssignedBy(assignment.getAssignedBy());
        entity.setAssignedAt(assignment.getAssignedAt() != null ? assignment.getAssignedAt() : java.time.Instant.now());

        LabAssignmentJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<LabAssignment> findByTemplateIdAndUserId(UUID templateId, UUID userId) {
        return jpaRepository.findByTemplateIdAndUserId(templateId, userId).map(this::toDomain);
    }

    @Override
    public List<LabAssignment> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<LabAssignment> findByTemplateId(UUID templateId) {
        return jpaRepository.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId) {
        return jpaRepository.existsByTemplateIdAndUserId(templateId, userId);
    }

    @Override
    public void deleteByTemplateIdAndUserId(UUID templateId, UUID userId) {
        jpaRepository.deleteByTemplateIdAndUserId(templateId, userId);
    }

    @Override
    public List<LabAssignment> findByAssignedBy(UUID assignedBy) {
        return jpaRepository.findByAssignedBy(assignedBy).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<LabAssignment> findByTemplateIdAndAssignedBy(UUID templateId, UUID assignedBy) {
        return jpaRepository.findByTemplateIdAndAssignedBy(templateId, assignedBy).stream()
                .map(this::toDomain)
                .toList();
    }

    private LabAssignment toDomain(LabAssignmentJpaEntity entity) {
        return LabAssignment.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .userId(entity.getUserId())
                .assignedBy(entity.getAssignedBy())
                .assignedAt(entity.getAssignedAt())
                .build();
    }
}
