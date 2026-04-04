package com.mnco.application.dto.response;

import com.mnco.domain.entities.LabStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public projection of a Lab — safe for REST responses.
 */
public record LabResponse(
        UUID id,
        String name,
        String description,
        LabStatus status,
        UUID ownerId,
        UUID templateId,
        String evengLabId,
        int cpuAllocated,
        int ramAllocated,
        int storageAllocated,
        Instant startedAt,
        Instant stoppedAt,
        Instant createdAt
) {}
