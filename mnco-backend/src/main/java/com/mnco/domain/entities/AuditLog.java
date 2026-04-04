package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable, append-only audit log entry.
 *
 * Covers two SRS requirements:
 *   FR-AA-07 — authentication events (LOGIN, LOGIN_FAILED, TOKEN_REFRESH, LOGOUT)
 *   FR-LM-10 — lab lifecycle events (LAB_CREATED, LAB_STARTED, LAB_STOPPED,
 *                                    LAB_CLONED, LAB_DELETED, NODE_STARTED, NODE_STOPPED)
 *
 * Design: no UPDATE or DELETE is ever issued on this entity.
 * The DB trigger enforces this at the database level (see V3 migration).
 */
public class AuditLog {

    public enum EventType {
        // Auth events (FR-AA-07)
        LOGIN, LOGIN_FAILED, TOKEN_REFRESH, LOGOUT,
        // Lab lifecycle events (FR-LM-10)
        LAB_CREATED, LAB_STARTED, LAB_STOPPED,
        LAB_CLONED, LAB_DELETED,
        NODE_STARTED, NODE_STOPPED
    }

    public enum Result {
        SUCCESS, FAILURE
    }

    private UUID id;
    private EventType eventType;
    private UUID actorId;           // user who performed the action
    private String actorUsername;
    private UUID labId;             // null for auth events
    private String labName;
    private Result result;
    private String errorCode;       // null on SUCCESS
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId()                     { return id; }
    public void setId(UUID id)              { this.id = id; }

    public EventType getEventType()                  { return eventType; }
    public void setEventType(EventType eventType)    { this.eventType = eventType; }

    public UUID getActorId()                { return actorId; }
    public void setActorId(UUID actorId)    { this.actorId = actorId; }

    public String getActorUsername()                    { return actorUsername; }
    public void setActorUsername(String actorUsername)  { this.actorUsername = actorUsername; }

    public UUID getLabId()                  { return labId; }
    public void setLabId(UUID labId)        { this.labId = labId; }

    public String getLabName()              { return labName; }
    public void setLabName(String labName)  { this.labName = labName; }

    public Result getResult()               { return result; }
    public void setResult(Result result)    { this.result = result; }

    public String getErrorCode()                { return errorCode; }
    public void setErrorCode(String errorCode)  { this.errorCode = errorCode; }

    public String getIpAddress()                { return ipAddress; }
    public void setIpAddress(String ipAddress)  { this.ipAddress = ipAddress; }

    public String getUserAgent()                { return userAgent; }
    public void setUserAgent(String userAgent)  { this.userAgent = userAgent; }

    public Instant getCreatedAt()               { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // ── Factory methods ───────────────────────────────────────────────────────

    public static AuditLog authEvent(EventType type, UUID actorId, String username,
                                     Result result, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setEventType(type);
        log.setActorId(actorId);
        log.setActorUsername(username);
        log.setResult(result);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setCreatedAt(Instant.now());
        return log;
    }

    public static AuditLog labEvent(EventType type, UUID actorId, String username,
                                    UUID labId, String labName, Result result) {
        AuditLog log = new AuditLog();
        log.setEventType(type);
        log.setActorId(actorId);
        log.setActorUsername(username);
        log.setLabId(labId);
        log.setLabName(labName);
        log.setResult(result);
        log.setCreatedAt(Instant.now());
        return log;
    }
}
