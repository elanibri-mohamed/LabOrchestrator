package com.mnco.presentation.controller;

import com.mnco.application.dto.request.AssignStudentRequest;
import com.mnco.application.dto.response.*;
import com.mnco.application.usecases.MultiTenantLabService;
import com.mnco.domain.entities.UserRole;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import com.mnco.security.service.UserDetailsServiceImpl.MncoUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/eveng/labs", produces = MediaType.APPLICATION_JSON_VALUE)
public class EvengLabController {

    private final MultiTenantLabService multiTenantLabService;

    // ── Template listing ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvengTemplateResponse>>> listAccessibleTemplates(
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.debug("GET /eveng/labs — user={}", principal.getUserId());
        List<EvengTemplateResponse> templates = multiTenantLabService.getAccessibleTemplates(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    // ── Assign to student (TEACHER only) ────────────────────────────────────────

    @PostMapping("/{templateId}/assign/student")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> assignToStudent(
            @PathVariable UUID templateId,
            @Valid @RequestBody AssignStudentRequest request,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /eveng/labs/{}/assign/student — teacher={}, student={}", templateId, principal.getUserId(), request.studentId());
        multiTenantLabService.assignToStudent(templateId, request.studentId(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Student assigned successfully"));
    }

    // ── Revoke assignment (ADMIN or TEACHER) ────────────────────────────────────

    @DeleteMapping("/{templateId}/assignments/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> revokeAssignment(
            @PathVariable UUID templateId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("DELETE /eveng/labs/{}/assignments/{} — requester={}", templateId, userId, principal.getUserId());
        multiTenantLabService.revokeAssignment(templateId, userId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ── Instance lifecycle ──────────────────────────────────────────────────────

    @PostMapping("/{templateId}/start")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','STUDENT','RESEARCHER')")
    public ResponseEntity<ApiResponse<LabInstanceResponse>> startInstance(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /eveng/labs/{}/start — user={}", templateId, principal.getUserId());
        LabInstanceResponse resp = multiTenantLabService.startInstance(templateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lab started", resp));
    }

    @PostMapping("/{templateId}/stop")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LabInstanceResponse>> stopOwnInstance(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /eveng/labs/{}/stop — teacher={}", templateId, principal.getUserId());
        LabInstanceResponse resp = multiTenantLabService.stopOwnInstance(templateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lab stopped", resp));
    }

    @PostMapping("/{templateId}/students/{studentId}/stop")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LabInstanceResponse>> stopStudentInstance(
            @PathVariable UUID templateId,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /eveng/labs/{}/students/{}/stop — teacher={}", templateId, studentId, principal.getUserId());
        LabInstanceResponse resp = multiTenantLabService.stopStudentInstance(templateId, studentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Student lab stopped", resp));
    }

    @PostMapping("/{templateId}/reset")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','STUDENT','RESEARCHER')")
    public ResponseEntity<ApiResponse<LabInstanceResponse>> resetInstance(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /eveng/labs/{}/reset — user={}", templateId, principal.getUserId());
        LabInstanceResponse resp = multiTenantLabService.resetInstance(templateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lab reset", resp));
    }

    @PostMapping("/{templateId}/students/{studentId}/reset")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LabInstanceResponse>> resetStudentInstance(
            @PathVariable UUID templateId,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /eveng/labs/{}/students/{}/reset — teacher={}", templateId, studentId, principal.getUserId());
        LabInstanceResponse resp = multiTenantLabService.resetStudentInstance(templateId, studentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Student lab reset", resp));
    }

    // ── Console access ──────────────────────────────────────────────────────────

    @GetMapping("/{templateId}/nodes/{nodeId}/console")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','STUDENT','RESEARCHER')")
    public ResponseEntity<ApiResponse<EveNgNodeConsoleInfo>> getConsoleUrl(
            @PathVariable UUID templateId,
            @PathVariable String nodeId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.debug("GET console: template={}, node={}, user={}", templateId, nodeId, principal.getUserId());
        EveNgNodeConsoleInfo info = multiTenantLabService.getConsoleUrl(templateId, principal.getUserId(), nodeId);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    // ── Progress monitoring ─────────────────────────────────────────────────────

    @GetMapping("/{templateId}/progress")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<StudentProgressReport>>> getProgress(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.debug("GET /eveng/labs/{}/progress — teacher={}", templateId, principal.getUserId());
        List<StudentProgressReport> reports = multiTenantLabService.getProgress(templateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/{templateId}/students/instances")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<LabInstanceResponse>>> listStudentInstances(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.debug("GET /eveng/labs/{}/students/instances — teacher={}", templateId, principal.getUserId());
        List<LabInstanceResponse> instances = multiTenantLabService.listStudentInstances(templateId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(instances));
    }
}
