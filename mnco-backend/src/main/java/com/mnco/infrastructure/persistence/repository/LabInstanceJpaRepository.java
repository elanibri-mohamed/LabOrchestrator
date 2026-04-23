package com.mnco.infrastructure.persistence.repository;

import com.mnco.domain.entities.InstanceStatus;
import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabInstanceJpaRepository extends JpaRepository<LabInstanceJpaEntity, UUID> {

    Optional<LabInstanceJpaEntity> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabInstanceJpaEntity> findByUserId(UUID userId);

    List<LabInstanceJpaEntity> findByTemplateId(UUID templateId);

    List<LabInstanceJpaEntity> findByStatus(InstanceStatus status);

    boolean existsByTemplateIdAndUserId(UUID templateId, UUID userId);

    void deleteByTemplateIdAndUserId(UUID templateId, UUID userId);

    List<LabInstanceJpaEntity> findByUserIdAndStatus(UUID userId, InstanceStatus status);
}
