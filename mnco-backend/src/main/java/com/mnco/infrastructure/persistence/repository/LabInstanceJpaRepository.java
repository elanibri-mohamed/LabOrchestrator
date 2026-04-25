package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabInstanceJpaRepository extends JpaRepository<LabInstanceJpaEntity, UUID> {

    Optional<LabInstanceJpaEntity> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    Optional<LabInstanceJpaEntity> findByEveInstancePath(String path);

    java.util.List<LabInstanceJpaEntity> findByStatusAndStartedAtBefore(com.mnco.domain.entities.InstanceStatus status, java.time.Instant threshold);
}
