package com.mnco.application.dto.response;

/**
 * Result of a sync operation from EVE-NG.
 */
public record SyncResultResponse(
        int created,
        int updated,
        int removed
) {}
