package com.mnco.application.mapper;

import com.mnco.domain.entities.ResourceQuota;
import com.mnco.infrastructure.persistence.entity.ResourceQuotaJpaEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-07T13:20:23+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 25.0.2 (Oracle Corporation)"
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
        resourceQuotaJpaEntity.maxCpu( quota.getMaxCpu() );
        resourceQuotaJpaEntity.maxLabs( quota.getMaxLabs() );
        resourceQuotaJpaEntity.maxRamGb( quota.getMaxRamGb() );
        resourceQuotaJpaEntity.maxStorageGb( quota.getMaxStorageGb() );
        resourceQuotaJpaEntity.updatedAt( quota.getUpdatedAt() );
        resourceQuotaJpaEntity.usedCpu( quota.getUsedCpu() );
        resourceQuotaJpaEntity.usedLabs( quota.getUsedLabs() );
        resourceQuotaJpaEntity.usedRamGb( quota.getUsedRamGb() );
        resourceQuotaJpaEntity.usedStorageGb( quota.getUsedStorageGb() );
        resourceQuotaJpaEntity.userId( quota.getUserId() );

        return resourceQuotaJpaEntity.build();
    }
}
