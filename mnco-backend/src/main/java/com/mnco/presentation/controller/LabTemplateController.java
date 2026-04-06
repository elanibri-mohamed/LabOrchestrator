package com.mnco.presentation.controller;

import com.mnco.application.dto.request.CreateLabTemplateRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.LabTemplateResponse;
import com.mnco.application.usecases.LabTemplateUseCase;
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
@RequestMapping(value = "/api/v1/templates", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LabTemplateController {

    private final LabTemplateUseCase templateUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabTemplateResponse>>> listPublicTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getPublicTemplates()));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<LabTemplateResponse>>> listMyTemplates(
            @AuthenticationPrincipal MncoUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                templateUseCase.getMyTemplates(principal.getUserId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabTemplateResponse>> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getTemplateById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LabTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateLabTemplateRequest request,
            @AuthenticationPrincipal MncoUserDetails principal) {
        log.info("POST /api/v1/templates — author={}", principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created",
                        templateUseCase.createTemplate(request, principal.getUserId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal MncoUserDetails principal) {
        templateUseCase.deleteTemplate(id, principal.getUserId(), principal.isAdmin());
        return ResponseEntity.noContent().build();
    }
}