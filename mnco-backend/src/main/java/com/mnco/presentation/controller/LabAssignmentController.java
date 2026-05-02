package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.usecases.LabAssignmentUseCase;
import com.mnco.security.service.UserDetailsServiceImpl.MncoUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/assignments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LabAssignmentController {

    private final LabAssignmentUseCase assignmentUseCase;

    @PostMapping("/teacher/{teacherId}/template/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> assignToTeacher(
            @PathVariable UUID teacherId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("Admin {} assigning template {} to teacher {}", principal.getUserId(), templateId, teacherId);
        assignmentUseCase.assignToTeacher(templateId, teacherId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Assigned to teacher successfully"));
    }

    @PostMapping("/student/{studentId}/template/{templateId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> assignToStudent(
            @PathVariable UUID studentId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("User {} assigning template {} to student {}", principal.getUserId(), templateId, studentId);
        assignmentUseCase.assignToStudent(templateId, studentId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Assigned to student successfully"));
    }

    @DeleteMapping("/user/{userId}/template/{templateId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> revokeAssignment(
            @PathVariable UUID userId,
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("User {} revoking assignment for {} on template {}", principal.getUserId(), userId, templateId);
        assignmentUseCase.revokeAssignment(templateId, userId, principal.getUserId(), principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.success("Assignment revoked successfully"));
    }
}
