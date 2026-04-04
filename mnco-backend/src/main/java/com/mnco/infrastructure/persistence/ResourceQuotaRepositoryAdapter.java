package com.mnco.infrastructure.persistence;

import com.mnco.application.mapper.ResourceQuotaMapper;
import com.mnco.domain.entities.ResourceQuota;
import com.mnco.domain.repository.ResourceQuotaRepository;
import com.mnco.infrastructure.persistence.entity.ResourceQuotaJpaEntity;
import com.mnco.infrastructure.persistence.repository.ResourceQuotaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResourceQuotaRepositoryAdapter implements ResourceQuotaRepository {

    private final ResourceQuotaJpaRepository jpaRepository;
    private final ResourceQuotaMapper mapper;

    @Value("${quota.default-max-labs:3}")
    private int defaultMaxLabs;

    @Value("${quota.default-max-cpu:8}")
    private int defaultMaxCpu;

    @Value("${quota.default-max-ram-gb:16}")
    private int defaultMaxRamGb;

    @Value("${quota.default-max-storage-gb:50}")
    private int defaultMaxStorageGb;

    @Override
    public ResourceQuota save(ResourceQuota quota) {
        var entity = mapper.toJpaEntity(quota);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<ResourceQuota> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public ResourceQuota findOrCreateDefault(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain)
                .orElseGet(() -> {
                    var defaultEntity = ResourceQuotaJpaEntity.builder()
                            .userId(userId)
                            .maxLabs(defaultMaxLabs)
                            .maxCpu(defaultMaxCpu)
                            .maxRamGb(defaultMaxRamGb)
                            .maxStorageGb(defaultMaxStorageGb)
                            .usedLabs(0)
                            .usedCpu(0)
                            .usedRamGb(0)
                            .usedStorageGb(0)
                            .build();
                    return mapper.toDomain(jpaRepository.save(defaultEntity));
                });
    }
}
