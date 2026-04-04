package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.domain.entities.Lab;
import com.mnco.infrastructure.persistence.entity.LabJpaEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper bridging domain Lab ↔ JPA entity ↔ DTO.
 */
@Mapper(componentModel = "spring")
public interface LabMapper {

    LabResponse toResponse(Lab lab);

    Lab toDomain(LabJpaEntity entity);

    LabJpaEntity toJpaEntity(Lab lab);
}
