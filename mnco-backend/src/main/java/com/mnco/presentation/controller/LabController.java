package com.mnco.presentation.controller;

import com.mnco.application.dto.request.CloneLabRequest;
import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.usecases.LabUseCase;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import com.mnco.security.service.UserDetailsServiceImpl.MncoUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/labs", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LabController {

    private final LabUseCase labUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabResponse>>> listLabs(
            @AuthenticationPrincipal MncoUserDetails principal) {
        List<LabResponse> labs = principal.isAdmin()
                ? labUseCase.getAllLabs()
                : labUseCase.getLabsByOwner(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(labs));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LabResponse>> createLab(
            @Valid @RequestBody CreateLabRequest request,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /api/v1/labs — user={}", principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab created successfully",
                        labUseCase.createLab(request, principal.getUserId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabResponse>> getLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal MncoUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                labUseCase.getLabById(id, principal.getUserId(), principal.isAdmin())));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<LabResponse>> startLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /api/v1/labs/{}/start — user={}", id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lab started",
                labUseCase.startLab(id, principal.getUserId())));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<ApiResponse<LabResponse>> stopLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /api/v1/labs/{}/stop — user={}", id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lab stopped",
                labUseCase.stopLab(id, principal.getUserId())));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<LabResponse>> cloneLab(
            @PathVariable UUID id,
            @Valid @RequestBody CloneLabRequest request,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /api/v1/labs/{}/clone — user={}", id, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab cloned successfully",
                        labUseCase.cloneLab(id, request, principal.getUserId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("DELETE /api/v1/labs/{} — user={}", id, principal.getUserId());
        labUseCase.deleteLab(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/nodes/{nodeId}/console")
    public ResponseEntity<ApiResponse<EveNgNodeConsoleInfo>> getNodeConsole(
            @PathVariable UUID id,
            @PathVariable String nodeId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                labUseCase.getNodeConsoleInfo(id, nodeId, principal.getUserId())));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LabResponse>>> getAllLabsAdmin() {
        return ResponseEntity.ok(ApiResponse.success(labUseCase.getAllLabs()));
    }
}