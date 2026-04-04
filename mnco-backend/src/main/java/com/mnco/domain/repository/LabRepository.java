package com.mnco.domain.repository;

import com.mnco.domain.entities.Lab;
import com.mnco.domain.entities.LabStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for Lab persistence.
 */
public interface LabRepository {

    Lab save(Lab lab);

    Optional<Lab> findById(UUID id);

    List<Lab> findByOwnerId(UUID ownerId);

    List<Lab> findByStatus(LabStatus status);

    List<Lab> findAll();

    /**
     * Count labs by owner that are actively using resources (not DELETED/ERROR).
     */
    long countActiveLabsByOwner(UUID ownerId);

    /**
     * Find labs that have been idle beyond a certain threshold — used by the
     * auto-stop scheduler.
     */
    List<Lab> findRunningLabsIdleSince(Instant threshold);

    void deleteById(UUID id);
}
