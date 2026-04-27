package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.LabInstanceMapper;
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

@Component
@RequiredArgsConstructor
public class LabInstanceRepositoryAdapter implements LabInstanceRepository {

    private final LabInstanceJpaRepository jpaRepository;
    private final LabInstanceMapper mapper;

    @Override
    public LabInstance save(LabInstance instance) {
        LabInstanceJpaEntity entity = mapper.toJpaEntity(instance);
        LabInstanceJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<LabInstance> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<LabInstance> findByTemplateIdAndUserId(UUID templateId, UUID userId) {
        return jpaRepository.findByTemplateIdAndUserId(templateId, userId).map(mapper::toDomain);
    }

    @Override
    public Optional<LabInstance> findByEveInstancePath(String path) {
        return jpaRepository.findByEveInstancePath(path).map(mapper::toDomain);
    }

    @Override
    public Optional<LabInstance> findRunningByUserId(UUID userId) {
        return jpaRepository
                .findFirstByUserIdAndStatus(userId, com.mnco.domain.entities.InstanceStatus.RUNNING)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<LabInstance> findRunningLabsIdleSince(Instant threshold) {
        return jpaRepository
                .findByStatusAndStartedAtBefore(com.mnco.domain.entities.InstanceStatus.RUNNING, threshold)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LabInstance> findRunningLabsExpiredAtOrBefore(Instant threshold) {
        return jpaRepository
                .findByStatusAndExpiresAtLessThanEqual(com.mnco.domain.entities.InstanceStatus.RUNNING, threshold)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
