package com.mnco.presentation.controller;

import com.mnco.application.dto.request.CreateLabTemplateRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.LabTemplateResponse;
import com.mnco.application.usecases.LabTemplateUseCase;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.ResourceNotFoundException;
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
 * REST controller for lab template catalog management.
 *
 * Base path: /templates
 *
 * GET    /templates          — list all public templates (all roles)
 * GET    /templates/mine     — list my authored templates
 * GET    /templates/{id}     — get single template
 * POST   /templates          — create template (INSTRUCTOR, ADMIN)
 * DELETE /templates/{id}     — delete template (author or ADMIN)
 */
@Slf4j
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class LabTemplateController {

    private final LabTemplateUseCase templateUseCase;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabTemplateResponse>>> listPublicTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getPublicTemplates()));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<LabTemplateResponse>>> listMyTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getMyTemplates(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabTemplateResponse>> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getTemplateById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<LabTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateLabTemplateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID authorId = resolveUserId(userDetails.getUsername());
        log.info("POST /templates — author={}, name='{}'", authorId, request.name());
        LabTemplateResponse response = templateUseCase.createTemplate(request, authorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID requesterId = resolveUserId(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        templateUseCase.deleteTemplate(id, requesterId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Template deleted", null));
    }

    private UUID resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
