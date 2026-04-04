package com.mnco.application.mapper;

import com.mnco.domain.entities.ResourceQuota;
import com.mnco.infrastructure.persistence.entity.ResourceQuotaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceQuotaMapper {

    ResourceQuota toDomain(ResourceQuotaJpaEntity entity);

    ResourceQuotaJpaEntity toJpaEntity(ResourceQuota quota);
}
