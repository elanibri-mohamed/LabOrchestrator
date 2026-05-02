package com.mnco.infrastructure.persistence.repository;

import com.mnco.domain.entities.LabTemplateStatus;
import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabTemplateJpaRepository extends JpaRepository<LabTemplateJpaEntity, UUID> {

    Optional<LabTemplateJpaEntity> findByEveTemplatePath(String path);

    Optional<LabTemplateJpaEntity> findByName(String name);

    List<LabTemplateJpaEntity> findAllByStatus(LabTemplateStatus status);
}
