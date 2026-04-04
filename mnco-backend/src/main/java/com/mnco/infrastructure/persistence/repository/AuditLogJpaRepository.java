package com.mnco.infrastructure.persistence.repository;

import com.mnco.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    List<AuditLogJpaEntity> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    List<AuditLogJpaEntity> findByLabIdOrderByCreatedAtDesc(UUID labId);

    @Query("SELECT a FROM AuditLogJpaEntity a ORDER BY a.createdAt DESC")
    List<AuditLogJpaEntity> findRecent(PageRequest pageRequest);
}
