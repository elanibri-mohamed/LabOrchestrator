package com.mnco.domain.repository;

import com.mnco.domain.entities.AuditLog;

import java.util.List;
import java.util.UUID;

/**
 * Append-only port for audit log persistence.
 * No update or delete methods — by design.
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog log);

    List<AuditLog> findByActorId(UUID actorId);

    List<AuditLog> findByLabId(UUID labId);

    List<AuditLog> findRecent(int limit);
}
