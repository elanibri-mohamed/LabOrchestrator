package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.LabAssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabAssignmentJpaRepository extends JpaRepository<LabAssignmentJpaEntity, UUID> {

    Optional<LabAssignmentJpaEntity> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabAssignmentJpaEntity> findByUserId(UUID userId);

    List<LabAssignmentJpaEntity> findByTemplateId(UUID templateId);

    boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId);

    void deleteByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabAssignmentJpaEntity> findByAssignedBy(UUID assignedBy);

    List<LabAssignmentJpaEntity> findByTemplateIdAndAssignedBy(UUID templateId, UUID assignedBy);
}
