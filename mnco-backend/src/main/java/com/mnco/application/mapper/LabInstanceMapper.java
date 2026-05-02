package com.mnco.application.mapper;

import com.mnco.domain.entities.LabInstance;
import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for LabInstance.
 */
@Mapper(componentModel = "spring")
public interface LabInstanceMapper {

    @Mapping(target = "templateId", source = "template.id")
    LabInstance toDomain(LabInstanceJpaEntity entity);

    @Mapping(target = "template.id", source = "templateId")
    LabInstanceJpaEntity toJpaEntity(LabInstance instance);
}
