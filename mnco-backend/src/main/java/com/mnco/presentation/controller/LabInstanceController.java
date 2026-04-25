package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.usecases.LabInstanceUseCase;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import com.mnco.security.service.UserDetailsServiceImpl.MncoUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/instances", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LabInstanceController {

    private final LabInstanceUseCase instanceUseCase;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LabResponse>>> getMyInstances(
            @AuthenticationPrincipal MncoUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(instanceUseCase.getMyInstances(principal.getUserId())));
    }

    @PostMapping("/template/{templateId}/start")
    public ResponseEntity<ApiResponse<LabResponse>> startInstance(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("User {} starting instance for template {}", principal.getUserId(), templateId);
        return ResponseEntity.ok(ApiResponse.success("Lab started",
                instanceUseCase.startInstance(templateId, principal.getUserId())));
    }

    @PostMapping("/template/{templateId}/stop")
    public ResponseEntity<ApiResponse<LabResponse>> stopInstance(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("User {} stopping instance for template {}", principal.getUserId(), templateId);
        return ResponseEntity.ok(ApiResponse.success("Lab stopped",
                instanceUseCase.stopInstance(templateId, principal.getUserId())));
    }

    @PostMapping("/template/{templateId}/reset")
    public ResponseEntity<ApiResponse<LabResponse>> resetInstance(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("User {} resetting instance for template {}", principal.getUserId(), templateId);
        return ResponseEntity.ok(ApiResponse.success("Lab reset",
                instanceUseCase.resetInstance(templateId, principal.getUserId())));
    }

    @GetMapping("/template/{templateId}/nodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInstanceNodes(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                instanceUseCase.getInstanceNodes(templateId, principal.getUserId())));
    }

    @GetMapping("/template/{templateId}/nodes/{nodeId}/console")
    public ResponseEntity<ApiResponse<EveNgNodeConsoleInfo>> getNodeConsole(
            @PathVariable UUID templateId,
            @PathVariable String nodeId,
            @AuthenticationPrincipal MncoUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                instanceUseCase.getNodeConsoleInfo(templateId, nodeId, principal.getUserId())));
    }
}
