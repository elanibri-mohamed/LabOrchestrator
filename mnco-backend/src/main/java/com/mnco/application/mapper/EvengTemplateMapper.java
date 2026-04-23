package com.mnco.application.mapper;

import com.mnco.application.dto.response.EvengTemplateResponse;
import com.mnco.domain.entities.EvengTemplate;
import com.mnco.domain.entities.LabTemplateStatus;
import com.mnco.infrastructure.persistence.entity.EvengTemplateJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EvengTemplateMapper {

    @Mapping(source = "templateStatus", target = "status")
    EvengTemplate toDomain(EvengTemplateJpaEntity entity);

    @Mapping(source = "status", target = "templateStatus")
    EvengTemplateJpaEntity toJpaEntity(EvengTemplate domain);

    EvengTemplateResponse toResponse(EvengTemplate domain);

    List<EvengTemplateResponse> toResponseList(List<EvengTemplate> domains);
}
