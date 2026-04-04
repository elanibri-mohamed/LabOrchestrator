package com.mnco.infrastructure.external.eveng.model;

/**
 * Result returned after successfully creating a topology in EVE-NG.
 */
public record EveNgLabResult(
        String evengLabId,
        String evengNodeId,
        String labPath,
        String status
) {}
