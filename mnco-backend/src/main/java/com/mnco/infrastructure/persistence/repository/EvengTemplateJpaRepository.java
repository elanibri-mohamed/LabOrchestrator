package com.mnco.infrastructure.persistence.repository;

import com.mnco.domain.entities.LabTemplateStatus;
import com.mnco.infrastructure.persistence.entity.EvengTemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvengTemplateJpaRepository extends JpaRepository<EvengTemplateJpaEntity, UUID> {

    Optional<EvengTemplateJpaEntity> findByEvengTemplatePath(String evengTemplatePath);

    List<EvengTemplateJpaEntity> findByTemplateStatus(LabTemplateStatus status);

    List<EvengTemplateJpaEntity> findByNameContainingIgnoreCase(String name);
}
