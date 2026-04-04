package com.mnco.application.dto.response;

import com.mnco.domain.entities.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditLog.EventType eventType,
        UUID actorId,
        String actorUsername,
        UUID labId,
        String labName,
        AuditLog.Result result,
        String errorCode,
        String ipAddress,
        Instant createdAt
) {}
