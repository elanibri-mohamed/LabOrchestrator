package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.domain.entities.LabStatus;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper bridging domain LabTemplate ↔ JPA entity ↔ DTO.
 */
@Mapper(componentModel = "spring")
public interface LabTemplateMapper {

    @Mapping(target = "templateId", source = "id")
    @Mapping(target = "evengLabId", source = "eveTemplatePath")
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "status", constant = "STOPPED")
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "stoppedAt", ignore = true)
    LabResponse toResponse(LabTemplate template);

    LabTemplate toDomain(LabTemplateJpaEntity entity);

    LabTemplateJpaEntity toJpaEntity(LabTemplate template);
}
