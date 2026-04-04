package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.ResourceQuotaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceQuotaJpaRepository extends JpaRepository<ResourceQuotaJpaEntity, UUID> {

    Optional<ResourceQuotaJpaEntity> findByUserId(UUID userId);
}
