package com.mnco.application.usecases;

import com.mnco.domain.entities.AuditLog;
import com.mnco.domain.entities.AuditLog.EventType;
import com.mnco.domain.entities.AuditLog.Result;
import com.mnco.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Central service for writing audit log entries.
 *
 * All writes are @Async — audit logging must never slow down or fail
 * a user-facing request. Failures are logged as warnings, never propagated.
 *
 * Covers FR-AA-07 (auth events) and FR-LM-10 (lab lifecycle events).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ── Auth events (FR-AA-07) ────────────────────────────────────────────────

    @Async
    public void logLogin(UUID userId, String username, String ip, String userAgent) {
        persist(AuditLog.authEvent(EventType.LOGIN, userId, username,
                Result.SUCCESS, ip, userAgent));
    }

    @Async
    public void logLoginFailed(String identifier, String ip, String userAgent) {
        // Truncate to domain only to avoid storing PII (per SRS FR-AA-07)
        String safeName = truncateToDomain(identifier);
        persist(AuditLog.authEvent(EventType.LOGIN_FAILED, null, safeName,
                Result.FAILURE, ip, userAgent));
    }

    @Async
    public void logTokenRefresh(UUID userId, String username, String ip) {
        persist(AuditLog.authEvent(EventType.TOKEN_REFRESH, userId, username,
                Result.SUCCESS, ip, null));
    }

    @Async
    public void logLogout(UUID userId, String username, String ip) {
        persist(AuditLog.authEvent(EventType.LOGOUT, userId, username,
                Result.SUCCESS, ip, null));
    }

    // ── Lab lifecycle events (FR-LM-10) ──────────────────────────────────────

    @Async
    public void logLabEvent(EventType type, UUID actorId, String username,
                            UUID labId, String labName, Result result) {
        persist(AuditLog.labEvent(type, actorId, username, labId, labName, result));
    }

    @Async
    public void logLabEventFailure(EventType type, UUID actorId, String username,
                                   UUID labId, String labName, String errorCode) {
        AuditLog entry = AuditLog.labEvent(type, actorId, username, labId, labName, Result.FAILURE);
        entry.setErrorCode(errorCode);
        persist(entry);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void persist(AuditLog entry) {
        try {
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Audit failures must NEVER surface to the caller
            log.warn("Audit log persistence failed [{}]: {}", entry.getEventType(), ex.getMessage());
        }
    }

    /**
     * For failed login attempts: store domain portion only, not full email (PII protection).
     * "user@example.com" → "@example.com"
     */
    private String truncateToDomain(String identifier) {
        if (identifier != null && identifier.contains("@")) {
            return identifier.substring(identifier.indexOf('@'));
        }
        return "[username-redacted]";
    }
}
