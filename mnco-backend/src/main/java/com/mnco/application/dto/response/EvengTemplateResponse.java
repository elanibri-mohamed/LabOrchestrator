package com.mnco.application.dto.response;

import com.mnco.domain.entities.LabTemplateStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public DTO for an EVE-NG lab template.
 */
public record EvengTemplateResponse(
        UUID id,
        String name,
        String evengTemplatePath,
        LabTemplateStatus status,
        Instant lastSyncedAt,
        Instant createdAt,
        String description
) {}
