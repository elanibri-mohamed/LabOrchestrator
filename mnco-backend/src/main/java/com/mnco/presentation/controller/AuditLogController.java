package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.AuditLogResponse;
import com.mnco.domain.entities.AuditLog;
import com.mnco.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only audit log query endpoints (FR-AA-07, FR-LM-10).
 *
 * GET /audit/recent           — last N audit entries (default 100)
 * GET /audit/user/{userId}    — all audit entries for a specific user
 * GET /audit/lab/{labId}      — all audit entries for a specific lab
 */
@RestController
@RequestMapping("/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {
        List<AuditLogResponse> logs = auditLogRepository.findRecent(Math.min(limit, 500))
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByUser(
            @PathVariable UUID userId) {
        List<AuditLogResponse> logs = auditLogRepository.findByActorId(userId)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/lab/{labId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByLab(
            @PathVariable UUID labId) {
        List<AuditLogResponse> logs = auditLogRepository.findByLabId(labId)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(), log.getEventType(), log.getActorId(), log.getActorUsername(),
                log.getLabId(), log.getLabName(), log.getResult(), log.getErrorCode(),
                log.getIpAddress(), log.getCreatedAt()
        );
    }
}
