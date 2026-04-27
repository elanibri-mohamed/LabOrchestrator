package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.LabInstanceJpaEntity;
import com.mnco.domain.entities.InstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabInstanceJpaRepository extends JpaRepository<LabInstanceJpaEntity, UUID> {

    Optional<LabInstanceJpaEntity> findByTemplateIdAndUserId(UUID templateId, UUID userId);

    Optional<LabInstanceJpaEntity> findByEveInstancePath(String path);

    List<LabInstanceJpaEntity> findByStatusAndStartedAtBefore(InstanceStatus status, Instant threshold);

    Optional<LabInstanceJpaEntity> findFirstByUserIdAndStatus(UUID userId, InstanceStatus status);

    List<LabInstanceJpaEntity> findByStatusAndExpiresAtLessThanEqual(InstanceStatus status, Instant threshold);
}
