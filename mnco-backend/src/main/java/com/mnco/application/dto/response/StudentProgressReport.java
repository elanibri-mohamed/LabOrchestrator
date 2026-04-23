package com.mnco.application.dto.response;

import com.mnco.domain.entities.InstanceStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Progress report for a single student's lab instance.
 * Mirrors the reference StudentReport.
 */
public record StudentProgressReport(
        UUID studentId,
        String studentName,
        InstanceLifecycle instanceStatus,  // NOT_CREATED, STOPPED, RUNNING, ERROR
        Instant startedAt,
        Map<String, String> nodeStatuses,   // nodeId -> "RUNNING"/"STOPPED"
        Instant lastConnected,
        List<String> accessedNodes
) {
    /**
     * Instance lifecycle state (extends InstanceStatus to include NOT_CREATED).
     */
    public enum InstanceLifecycle {
        NOT_CREATED,
        STOPPED,
        RUNNING,
        ERROR
    }
}
