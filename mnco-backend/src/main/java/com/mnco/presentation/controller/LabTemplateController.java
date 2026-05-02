package com.mnco.presentation.controller;

import com.mnco.application.dto.request.UpdateLabDescriptionRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.usecases.LabTemplateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<ApiResponse<List<LabResponse>>> listTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getAllTemplates()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabResponse>> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(templateUseCase.getTemplateById(id)));
    }

    @PatchMapping("/{id}/description")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<LabResponse>> updateTemplateDescription(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLabDescriptionRequest request) {
        log.info("Updating template description for template={}", id);
        LabResponse updated = templateUseCase.updateTemplateDescription(id, request.description());
        return ResponseEntity.ok(ApiResponse.success("Template description updated", updated));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LabResponse>>> syncTemplates() {
        log.info("Admin initiated template sync from EVE-NG");
        List<LabResponse> synced = templateUseCase.discoverTemplatesFromEveNg();
        return ResponseEntity.ok(ApiResponse.success("Sync completed: " + synced.size() + " templates", synced));
    }
}