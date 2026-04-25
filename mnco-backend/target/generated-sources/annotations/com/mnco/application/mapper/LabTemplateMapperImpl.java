package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.domain.entities.LabStatus;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-26T00:27:39+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Amazon.com Inc.)"
)
@Component
public class LabTemplateMapperImpl implements LabTemplateMapper {

    @Override
    public LabResponse toResponse(LabTemplate template) {
        if ( template == null ) {
            return null;
        }

        UUID templateId = null;
        String evengLabId = null;
        UUID id = null;
        String name = null;
        String description = null;
        int cpuAllocated = 0;
        int ramAllocated = 0;
        int storageAllocated = 0;
        Instant createdAt = null;

        templateId = template.getId();
        evengLabId = template.getEveTemplatePath();
        id = template.getId();
        name = template.getName();
        description = template.getDescription();
        cpuAllocated = template.getCpuAllocated();
        ramAllocated = template.getRamAllocated();
        storageAllocated = template.getStorageAllocated();
        createdAt = template.getCreatedAt();

        UUID ownerId = null;
        LabStatus status = LabStatus.STOPPED;
        Instant startedAt = null;
        Instant stoppedAt = null;

        LabResponse labResponse = new LabResponse( id, name, description, status, ownerId, templateId, evengLabId, cpuAllocated, ramAllocated, storageAllocated, startedAt, stoppedAt, createdAt );

        return labResponse;
    }

    @Override
    public LabTemplate toDomain(LabTemplateJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LabTemplate.Builder labTemplate = LabTemplate.builder();

        labTemplate.id( entity.getId() );
        labTemplate.name( entity.getName() );
        labTemplate.description( entity.getDescription() );
        labTemplate.eveTemplatePath( entity.getEveTemplatePath() );
        labTemplate.status( entity.getStatus() );
        labTemplate.cpuAllocated( entity.getCpuAllocated() );
        labTemplate.ramAllocated( entity.getRamAllocated() );
        labTemplate.storageAllocated( entity.getStorageAllocated() );
        labTemplate.lastSyncedAt( entity.getLastSyncedAt() );
        labTemplate.createdAt( entity.getCreatedAt() );
        labTemplate.updatedAt( entity.getUpdatedAt() );

        return labTemplate.build();
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
        labTemplateJpaEntity.eveTemplatePath( template.getEveTemplatePath() );
        labTemplateJpaEntity.status( template.getStatus() );
        labTemplateJpaEntity.cpuAllocated( template.getCpuAllocated() );
        labTemplateJpaEntity.ramAllocated( template.getRamAllocated() );
        labTemplateJpaEntity.storageAllocated( template.getStorageAllocated() );
        labTemplateJpaEntity.lastSyncedAt( template.getLastSyncedAt() );
        labTemplateJpaEntity.createdAt( template.getCreatedAt() );
        labTemplateJpaEntity.updatedAt( template.getUpdatedAt() );

        return labTemplateJpaEntity.build();
    }
}
