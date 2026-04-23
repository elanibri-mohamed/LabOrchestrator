package com.mnco.infrastructure.persistence;

import com.mnco.domain.entities.InstanceStatus;
import com.mnco.domain.entities.LabInstance;
import com.mnco.domain.repository.LabInstanceRepository;
import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import com.mnco.infrastructure.persistence.repository.LabInstanceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter for LabInstanceRepository using JPA.
 */
@Component
@RequiredArgsConstructor
public class LabInstanceRepositoryAdapter implements LabInstanceRepository {

    private final LabInstanceJpaRepository jpaRepository;

    @Override
    public LabInstance save(LabInstance instance) {
        LabInstanceJpaEntity entity = toJpaEntity(instance);
        LabInstanceJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<LabInstance> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<LabInstance> findByTemplateIdAndUserId(UUID templateId, UUID userId) {
        return jpaRepository.findByTemplateIdAndUserId(templateId, userId).map(this::toDomain);
    }

    @Override
    public List<LabInstance> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<LabInstance> findByTemplateId(UUID templateId) {
        return jpaRepository.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<LabInstance> findByStatus(InstanceStatus status) {
        return jpaRepository.findByStatus(status).stream()
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
    public List<LabInstance> findByUserIdAndStatus(UUID userId, InstanceStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toDomain)
                .toList();
    }

    private LabInstance toDomain(LabInstanceJpaEntity entity) {
        return LabInstance.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .userId(entity.getUserId())
                .evengInstancePath(entity.getEvengInstancePath())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .stoppedAt(entity.getStoppedAt())
                .build();
    }

    private LabInstanceJpaEntity toJpaEntity(LabInstance domain) {
        return LabInstanceJpaEntity.builder()
                .id(domain.getId())
                .templateId(domain.getTemplateId())
                .userId(domain.getUserId())
                .evengInstancePath(domain.getEvengInstancePath())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .startedAt(domain.getStartedAt())
                .stoppedAt(domain.getStoppedAt())
                .build();
    }
}
