package com.mnco.presentation.controller;

import com.mnco.application.dto.request.CloneLabRequest;
import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.usecases.LabUseCase;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for lab lifecycle management.
 *
 * Endpoints:
 *   GET    /labs                          — list user's labs
 *   POST   /labs                          — create lab
 *   GET    /labs/{id}                     — get lab
 *   POST   /labs/{id}/start               — start lab
 *   POST   /labs/{id}/stop                — stop lab
 *   POST   /labs/{id}/clone               — clone lab (FR-LM-06)
 *   DELETE /labs/{id}                     — delete lab
 *   GET    /labs/{id}/nodes/{nodeId}/console — get console URL (FR-LM-09)
 *   GET    /labs/admin/all                — admin: all labs
 */
@Slf4j
@RestController
@RequestMapping("/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabUseCase labUseCase;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabResponse>>> listLabs(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = isAdmin(userDetails);
        List<LabResponse> labs = isAdmin
                ? labUseCase.getAllLabs()
                : labUseCase.getLabsByOwner(resolveUserId(userDetails.getUsername()));
        return ResponseEntity.ok(ApiResponse.success(labs));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LabResponse>> createLab(
            @Valid @RequestBody CreateLabRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = resolveUserId(userDetails.getUsername());
        log.info("POST /labs — user={}, name='{}'", ownerId, request.name());
        LabResponse lab = labUseCase.createLab(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab created successfully", lab));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabResponse>> getLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = resolveUserId(userDetails.getUsername());
        LabResponse lab = labUseCase.getLabById(id, requesterId, isAdmin(userDetails));
        return ResponseEntity.ok(ApiResponse.success(lab));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<LabResponse>> startLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = resolveUserId(userDetails.getUsername());
        log.info("POST /labs/{}/start — user={}", id, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Lab started", labUseCase.startLab(id, requesterId)));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<ApiResponse<LabResponse>> stopLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = resolveUserId(userDetails.getUsername());
        log.info("POST /labs/{}/stop — user={}", id, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Lab stopped", labUseCase.stopLab(id, requesterId)));
    }

    /**
     * Clone a STOPPED lab into a new independent lab (FR-LM-06).
     * Source must be STOPPED; clone inherits same resource profile.
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<LabResponse>> cloneLab(
            @PathVariable UUID id,
            @Valid @RequestBody CloneLabRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = resolveUserId(userDetails.getUsername());
        log.info("POST /labs/{}/clone — user={}, cloneName='{}'", id, requesterId, request.name());
        LabResponse clone = labUseCase.cloneLab(id, request, requesterId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab cloned successfully", clone));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLab(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = resolveUserId(userDetails.getUsername());
        log.info("DELETE /labs/{} — user={}", id, requesterId);
        labUseCase.deleteLab(id, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Lab deleted successfully", null));
    }

    /**
     * Get console connection details for a specific node (FR-LM-09).
     * Lab must be RUNNING; returns protocol, host, port, and WebSocket URL.
     */
    @GetMapping("/{id}/nodes/{nodeId}/console")
    public ResponseEntity<ApiResponse<EveNgNodeConsoleInfo>> getNodeConsole(
            @PathVariable UUID id,
            @PathVariable String nodeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = resolveUserId(userDetails.getUsername());
        log.debug("GET /labs/{}/nodes/{}/console — user={}", id, nodeId, requesterId);
        EveNgNodeConsoleInfo consoleInfo = labUseCase.getNodeConsoleInfo(id, nodeId, requesterId);
        return ResponseEntity.ok(ApiResponse.success(consoleInfo));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LabResponse>>> getAllLabsAdmin() {
        return ResponseEntity.ok(ApiResponse.success(labUseCase.getAllLabs()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
