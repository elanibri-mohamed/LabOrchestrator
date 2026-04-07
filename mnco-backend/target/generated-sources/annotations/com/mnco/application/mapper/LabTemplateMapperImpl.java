package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabTemplateResponse;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-08T00:31:47+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Amazon.com Inc.)"
)
@Component
public class LabTemplateMapperImpl implements LabTemplateMapper {

    @Override
    public LabTemplateResponse toResponse(LabTemplate template) {
        if ( template == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String description = null;
        String version = null;
        UUID authorId = null;
        Instant createdAt = null;

        id = template.getId();
        name = template.getName();
        description = template.getDescription();
        version = template.getVersion();
        authorId = template.getAuthorId();
        createdAt = template.getCreatedAt();

        boolean isPublic = false;

        LabTemplateResponse labTemplateResponse = new LabTemplateResponse( id, name, description, version, authorId, isPublic, createdAt );

        return labTemplateResponse;
    }

    @Override
    public LabTemplate toDomain(LabTemplateJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LabTemplate labTemplate = new LabTemplate();

        labTemplate.setId( entity.getId() );
        labTemplate.setName( entity.getName() );
        labTemplate.setDescription( entity.getDescription() );
        labTemplate.setTopologyYaml( entity.getTopologyYaml() );
        labTemplate.setVersion( entity.getVersion() );
        labTemplate.setAuthorId( entity.getAuthorId() );
        labTemplate.setPublic( entity.isPublic() );
        labTemplate.setCreatedAt( entity.getCreatedAt() );
        labTemplate.setUpdatedAt( entity.getUpdatedAt() );

        return labTemplate;
    }

    @Override
    public LabTemplateJpaEntity toJpaEntity(LabTemplate template) {
        if ( template == null ) {
            return null;
        }

        LabTemplateJpaEntity.LabTemplateJpaEntityBuilder labTemplateJpaEntity = LabTemplateJpaEntity.builder();

        labTemplateJpaEntity.id( template.getId() );
        labTemplateJpaEntity.name( template.getName() );
        labTemplateJpaEntity.description( template.getDescription() );
        labTemplateJpaEntity.topologyYaml( template.getTopologyYaml() );
        labTemplateJpaEntity.version( template.getVersion() );
        labTemplateJpaEntity.authorId( template.getAuthorId() );
        labTemplateJpaEntity.createdAt( template.getCreatedAt() );
        labTemplateJpaEntity.updatedAt( template.getUpdatedAt() );

        return labTemplateJpaEntity.build();
    }
}
