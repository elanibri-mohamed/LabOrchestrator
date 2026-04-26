package com.mnco.application.mapper;

import com.mnco.domain.entities.LabAssignment;
import com.mnco.infrastructure.persistence.entity.LabAssignmentJpaEntity;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-26T19:58:36+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class LabAssignmentMapperImpl implements LabAssignmentMapper {

    @Override
    public LabAssignment toDomain(LabAssignmentJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LabAssignment.Builder labAssignment = LabAssignment.builder();

        labAssignment.templateId( entityTemplateId( entity ) );
        labAssignment.id( entity.getId() );
        labAssignment.userId( entity.getUserId() );
        labAssignment.assignedBy( entity.getAssignedBy() );
        labAssignment.assignedAt( entity.getAssignedAt() );

        return labAssignment.build();
    }

    @Override
    public LabAssignmentJpaEntity toJpaEntity(LabAssignment assignment) {
        if ( assignment == null ) {
            return null;
        }

        LabAssignmentJpaEntity.LabAssignmentJpaEntityBuilder labAssignmentJpaEntity = LabAssignmentJpaEntity.builder();

        labAssignmentJpaEntity.template( labAssignmentToLabTemplateJpaEntity( assignment ) );
        labAssignmentJpaEntity.id( assignment.getId() );
        labAssignmentJpaEntity.userId( assignment.getUserId() );
        labAssignmentJpaEntity.assignedBy( assignment.getAssignedBy() );
        labAssignmentJpaEntity.assignedAt( assignment.getAssignedAt() );

        return labAssignmentJpaEntity.build();
    }

    private UUID entityTemplateId(LabAssignmentJpaEntity labAssignmentJpaEntity) {
        if ( labAssignmentJpaEntity == null ) {
            return null;
        }
        LabTemplateJpaEntity template = labAssignmentJpaEntity.getTemplate();
        if ( template == null ) {
            return null;
        }
        UUID id = template.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected LabTemplateJpaEntity labAssignmentToLabTemplateJpaEntity(LabAssignment labAssignment) {
        if ( labAssignment == null ) {
            return null;
        }

        LabTemplateJpaEntity.LabTemplateJpaEntityBuilder labTemplateJpaEntity = LabTemplateJpaEntity.builder();

        labTemplateJpaEntity.id( labAssignment.getTemplateId() );

        return labTemplateJpaEntity.build();
    }
}
