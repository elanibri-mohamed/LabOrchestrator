package com.mnco.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Admin-only payload to override a user's resource quota limits.
 */
public record UpdateQuotaRequest(

        @Min(1) @Max(50)
        int maxLabs,

        @Min(1) @Max(128)
        int maxCpu,

        @Min(1) @Max(512)
        int maxRamGb,

        @Min(10) @Max(2000)
        int maxStorageGb
) {}
