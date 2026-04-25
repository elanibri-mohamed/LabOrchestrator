package com.mnco.application.mapper;

import com.mnco.domain.entities.LabAssignment;
import com.mnco.infrastructure.persistence.entity.LabAssignmentJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for LabAssignment.
 */
@Mapper(componentModel = "spring")
public interface LabAssignmentMapper {

    @Mapping(target = "templateId", source = "template.id")
    LabAssignment toDomain(LabAssignmentJpaEntity entity);

    @Mapping(target = "template.id", source = "templateId")
    LabAssignmentJpaEntity toJpaEntity(LabAssignment assignment);
}
