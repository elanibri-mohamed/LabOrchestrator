package com.mnco.application.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Public projection of a user's resource quota — used in admin and profile endpoints.
 */
public record QuotaResponse(
        UUID userId,
        int maxLabs,      int usedLabs,      int remainingLabs,
        int maxCpu,       int usedCpu,       int remainingCpu,
        int maxRamGb,     int usedRamGb,     int remainingRamGb,
        int maxStorageGb, int usedStorageGb, int remainingStorageGb,
        Instant updatedAt
) {}
