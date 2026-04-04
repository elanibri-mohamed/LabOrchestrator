package com.mnco.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LabTemplateResponse(
        UUID id,
        String name,
        String description,
        String version,
        UUID authorId,
        boolean isPublic,
        Instant createdAt
) {}
