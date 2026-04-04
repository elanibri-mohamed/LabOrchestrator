package com.mnco.domain.repository;

import com.mnco.domain.entities.ResourceQuota;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level port for ResourceQuota persistence.
 */
public interface ResourceQuotaRepository {

    ResourceQuota save(ResourceQuota quota);

    Optional<ResourceQuota> findByUserId(UUID userId);

    /**
     * Finds or creates a default quota for the given user.
     * Used to ensure every user always has a quota record.
     */
    ResourceQuota findOrCreateDefault(UUID userId);
}
