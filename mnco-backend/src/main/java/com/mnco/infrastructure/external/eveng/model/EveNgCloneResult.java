package com.mnco.infrastructure.external.eveng.model;

/**
 * Result of a deep-copy clone operation on EVE-NG.
 */
public record EveNgCloneResult(
        String clonedEvengLabId,
        String clonedLabPath,
        String status
) {}
