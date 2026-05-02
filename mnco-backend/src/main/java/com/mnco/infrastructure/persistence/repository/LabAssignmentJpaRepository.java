package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.LabAssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabAssignmentJpaRepository extends JpaRepository<LabAssignmentJpaEntity, UUID> {

    List<LabAssignmentJpaEntity> findAllByUserId(UUID userId);

    Optional<LabAssignmentJpaEntity> findByTemplateIdAndUserId(UUID templateId, UUID userId);
    
    boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId);
}
