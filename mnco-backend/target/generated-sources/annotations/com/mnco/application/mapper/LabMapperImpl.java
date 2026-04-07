package com.mnco.application.mapper;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.domain.entities.Lab;
import com.mnco.domain.entities.LabStatus;
import com.mnco.infrastructure.persistence.entity.LabJpaEntity;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-08T00:31:48+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Amazon.com Inc.)"
)
@Component
public class LabMapperImpl implements LabMapper {

    @Override
    public LabResponse toResponse(Lab lab) {
        if ( lab == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        String description = null;
        LabStatus status = null;
        UUID ownerId = null;
        UUID templateId = null;
        String evengLabId = null;
        int cpuAllocated = 0;
        int ramAllocated = 0;
        int storageAllocated = 0;
        Instant startedAt = null;
        Instant stoppedAt = null;
        Instant createdAt = null;

        id = lab.getId();
        name = lab.getName();
        description = lab.getDescription();
        status = lab.getStatus();
        ownerId = lab.getOwnerId();
        templateId = lab.getTemplateId();
        evengLabId = lab.getEvengLabId();
        cpuAllocated = lab.getCpuAllocated();
        ramAllocated = lab.getRamAllocated();
        storageAllocated = lab.getStorageAllocated();
        startedAt = lab.getStartedAt();
        stoppedAt = lab.getStoppedAt();
        createdAt = lab.getCreatedAt();

        LabResponse labResponse = new LabResponse( id, name, description, status, ownerId, templateId, evengLabId, cpuAllocated, ramAllocated, storageAllocated, startedAt, stoppedAt, createdAt );

        return labResponse;
    }

    @Override
    public Lab toDomain(LabJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Lab.Builder lab = Lab.builder();

        lab.id( entity.getId() );
        lab.name( entity.getName() );
        lab.description( entity.getDescription() );
        lab.status( entity.getStatus() );
        lab.ownerId( entity.getOwnerId() );
        lab.templateId( entity.getTemplateId() );
        lab.evengLabId( entity.getEvengLabId() );
        lab.evengNodeId( entity.getEvengNodeId() );
        lab.cpuAllocated( entity.getCpuAllocated() );
        lab.ramAllocated( entity.getRamAllocated() );
        lab.storageAllocated( entity.getStorageAllocated() );
        lab.startedAt( entity.getStartedAt() );
        lab.stoppedAt( entity.getStoppedAt() );
        lab.lastActiveAt( entity.getLastActiveAt() );
        lab.createdAt( entity.getCreatedAt() );
        lab.updatedAt( entity.getUpdatedAt() );

        return lab.build();
    }

    @Override
    public LabJpaEntity toJpaEntity(Lab lab) {
        if ( lab == null ) {
            return null;
        }

        LabJpaEntity.LabJpaEntityBuilder labJpaEntity = LabJpaEntity.builder();

        labJpaEntity.id( lab.getId() );
        labJpaEntity.name( lab.getName() );
        labJpaEntity.description( lab.getDescription() );
        labJpaEntity.status( lab.getStatus() );
        labJpaEntity.ownerId( lab.getOwnerId() );
        labJpaEntity.templateId( lab.getTemplateId() );
        labJpaEntity.evengLabId( lab.getEvengLabId() );
        labJpaEntity.evengNodeId( lab.getEvengNodeId() );
        labJpaEntity.cpuAllocated( lab.getCpuAllocated() );
        labJpaEntity.ramAllocated( lab.getRamAllocated() );
        labJpaEntity.storageAllocated( lab.getStorageAllocated() );
        labJpaEntity.startedAt( lab.getStartedAt() );
        labJpaEntity.stoppedAt( lab.getStoppedAt() );
        labJpaEntity.lastActiveAt( lab.getLastActiveAt() );
        labJpaEntity.createdAt( lab.getCreatedAt() );
        labJpaEntity.updatedAt( lab.getUpdatedAt() );

        return labJpaEntity.build();
    }
}
