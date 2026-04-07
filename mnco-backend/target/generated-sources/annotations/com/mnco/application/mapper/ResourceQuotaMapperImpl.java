package com.mnco.application.mapper;

import com.mnco.domain.entities.ResourceQuota;
import com.mnco.infrastructure.persistence.entity.ResourceQuotaJpaEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-08T00:31:48+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Amazon.com Inc.)"
)
@Component
public class ResourceQuotaMapperImpl implements ResourceQuotaMapper {

    @Override
    public ResourceQuota toDomain(ResourceQuotaJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ResourceQuota resourceQuota = new ResourceQuota();

        resourceQuota.setId( entity.getId() );
        resourceQuota.setUserId( entity.getUserId() );
        resourceQuota.setMaxLabs( entity.getMaxLabs() );
        resourceQuota.setMaxCpu( entity.getMaxCpu() );
        resourceQuota.setMaxRamGb( entity.getMaxRamGb() );
        resourceQuota.setMaxStorageGb( entity.getMaxStorageGb() );
        resourceQuota.setUsedLabs( entity.getUsedLabs() );
        resourceQuota.setUsedCpu( entity.getUsedCpu() );
        resourceQuota.setUsedRamGb( entity.getUsedRamGb() );
        resourceQuota.setUsedStorageGb( entity.getUsedStorageGb() );
        resourceQuota.setUpdatedAt( entity.getUpdatedAt() );

        return resourceQuota;
    }

    @Override
    public ResourceQuotaJpaEntity toJpaEntity(ResourceQuota quota) {
        if ( quota == null ) {
            return null;
        }

        ResourceQuotaJpaEntity.ResourceQuotaJpaEntityBuilder resourceQuotaJpaEntity = ResourceQuotaJpaEntity.builder();

        resourceQuotaJpaEntity.id( quota.getId() );
        resourceQuotaJpaEntity.userId( quota.getUserId() );
        resourceQuotaJpaEntity.maxLabs( quota.getMaxLabs() );
        resourceQuotaJpaEntity.maxCpu( quota.getMaxCpu() );
        resourceQuotaJpaEntity.maxRamGb( quota.getMaxRamGb() );
        resourceQuotaJpaEntity.maxStorageGb( quota.getMaxStorageGb() );
        resourceQuotaJpaEntity.usedLabs( quota.getUsedLabs() );
        resourceQuotaJpaEntity.usedCpu( quota.getUsedCpu() );
        resourceQuotaJpaEntity.usedRamGb( quota.getUsedRamGb() );
        resourceQuotaJpaEntity.usedStorageGb( quota.getUsedStorageGb() );
        resourceQuotaJpaEntity.updatedAt( quota.getUpdatedAt() );

        return resourceQuotaJpaEntity.build();
    }
}
