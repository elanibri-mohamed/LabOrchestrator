package com.mnco.application.dto.response;

import com.mnco.domain.entities.InstanceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public DTO for a lab instance (user's private copy).
 */
public record LabInstanceResponse(
        UUID id,
        UUID templateId,
        UUID userId,
        String evengInstancePath,
        InstanceStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant stoppedAt
) {}
