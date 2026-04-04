package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.LabTemplateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabTemplateJpaRepository extends JpaRepository<LabTemplateJpaEntity, UUID> {

    List<LabTemplateJpaEntity> findByIsPublicTrue();

    List<LabTemplateJpaEntity> findByAuthorId(UUID authorId);

    boolean existsByName(String name);
}
