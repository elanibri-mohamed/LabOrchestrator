package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabInstanceResponse;
import com.mnco.domain.entities.LabInstance;
import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LabInstanceMapper {

    LabInstance toDomain(LabInstanceJpaEntity entity);

    LabInstanceJpaEntity toJpaEntity(LabInstance domain);

    LabInstanceResponse toResponse(LabInstance domain);

    List<LabInstanceResponse> toResponseList(List<LabInstance> domains);
}
