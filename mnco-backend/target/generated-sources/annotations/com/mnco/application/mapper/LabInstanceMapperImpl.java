package com.mnco.application.mapper;

import com.mnco.domain.entities.LabInstance;
import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-26T19:57:41+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class LabInstanceMapperImpl implements LabInstanceMapper {

    @Override
    public LabInstance toDomain(LabInstanceJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LabInstance.Builder labInstance = LabInstance.builder();

        labInstance.templateId( entityTemplateId( entity ) );
        labInstance.id( entity.getId() );
        labInstance.userId( entity.getUserId() );
        labInstance.eveInstancePath( entity.getEveInstancePath() );
        labInstance.status( entity.getStatus() );
        labInstance.startedAt( entity.getStartedAt() );
        labInstance.stoppedAt( entity.getStoppedAt() );
        labInstance.createdAt( entity.getCreatedAt() );
        labInstance.updatedAt( entity.getUpdatedAt() );

        return labInstance.build();
    }

    @Override
    public LabInstanceJpaEntity toJpaEntity(LabInstance instance) {
        if ( instance == null ) {
            return null;
        }

        LabInstanceJpaEntity.LabInstanceJpaEntityBuilder labInstanceJpaEntity = LabInstanceJpaEntity.builder();

        labInstanceJpaEntity.template( labInstanceToLabTemplateJpaEntity( instance ) );
        labInstanceJpaEntity.id( instance.getId() );
        labInstanceJpaEntity.userId( instance.getUserId() );
        labInstanceJpaEntity.eveInstancePath( instance.getEveInstancePath() );
        labInstanceJpaEntity.status( instance.getStatus() );
        labInstanceJpaEntity.startedAt( instance.getStartedAt() );
        labInstanceJpaEntity.stoppedAt( instance.getStoppedAt() );
        labInstanceJpaEntity.createdAt( instance.getCreatedAt() );
        labInstanceJpaEntity.updatedAt( instance.getUpdatedAt() );

        return labInstanceJpaEntity.build();
    }

    private UUID entityTemplateId(LabInstanceJpaEntity labInstanceJpaEntity) {
        if ( labInstanceJpaEntity == null ) {
            return null;
        }
        LabTemplateJpaEntity template = labInstanceJpaEntity.getTemplate();
        if ( template == null ) {
            return null;
        }
        UUID id = template.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected LabTemplateJpaEntity labInstanceToLabTemplateJpaEntity(LabInstance labInstance) {
        if ( labInstance == null ) {
            return null;
        }

        LabTemplateJpaEntity.LabTemplateJpaEntityBuilder labTemplateJpaEntity = LabTemplateJpaEntity.builder();

        labTemplateJpaEntity.id( labInstance.getTemplateId() );

        return labTemplateJpaEntity.build();
    }
}
