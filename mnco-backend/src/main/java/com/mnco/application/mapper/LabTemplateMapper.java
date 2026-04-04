package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabTemplateResponse;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabTemplateMapper {

    LabTemplateResponse toResponse(LabTemplate template);

    LabTemplate toDomain(LabTemplateJpaEntity entity);

    LabTemplateJpaEntity toJpaEntity(LabTemplate template);
}
