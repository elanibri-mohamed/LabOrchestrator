package com.mnco.infrastructure.persistence.repository;

import com.mnco.domain.entities.LabStatus;
import com.mnco.infrastructure.persistence.entity.LabJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for Lab persistence.
 */
@Repository
public interface LabJpaRepository extends JpaRepository<LabJpaEntity, UUID> {

    List<LabJpaEntity> findByOwnerId(UUID ownerId);

    List<LabJpaEntity> findByStatus(LabStatus status);

    @Query("""
            SELECT COUNT(l) FROM LabJpaEntity l
            WHERE l.ownerId = :ownerId
            AND l.status NOT IN ('DELETED', 'ERROR', 'DELETING')
            """)
    long countActiveLabsByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
            SELECT l FROM LabJpaEntity l
            WHERE l.status = 'RUNNING'
            AND (l.lastActiveAt IS NULL OR l.lastActiveAt < :threshold)
            """)
    List<LabJpaEntity> findRunningLabsIdleSince(@Param("threshold") Instant threshold);
}
