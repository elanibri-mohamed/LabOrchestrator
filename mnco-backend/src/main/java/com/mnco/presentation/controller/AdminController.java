package com.mnco.presentation.controller;

import com.mnco.application.dto.request.UpdateQuotaRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.QuotaResponse;
import com.mnco.application.dto.response.UserResponse;
import com.mnco.application.mapper.UserMapper;
import com.mnco.domain.entities.ResourceQuota;
import com.mnco.domain.entities.UserRole;
import com.mnco.domain.repository.ResourceQuotaRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing admin-only management endpoints.
 *
 * All routes require ROLE_ADMIN (enforced both here and in SecurityConfig).
 *
 * GET    /admin/users                    — list all platform users
 * GET    /admin/users/{id}               — get a user by ID
 * PATCH  /admin/users/{id}/role          — change a user's role
 * DELETE /admin/users/{id}               — disable a user
 * GET    /admin/users/{id}/quota         — view a user's quota
 * PUT    /admin/users/{id}/quota         — override a user's quota limits
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ResourceQuotaRepository quotaRepository;
    private final UserMapper userMapper;

    // ── GET /admin/users ──────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listAllUsers() {
        log.info("ADMIN: listing all users");
        List<UserResponse> users = userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // ── GET /admin/users/{id} ─────────────────────────────────────────────────

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(user)));
    }

    // ── PATCH /admin/users/{id}/role ──────────────────────────────────────────

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable UUID id,
            @RequestParam UserRole role) {

        log.info("ADMIN: changing role of user={} to {}", id, role);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setRole(role);
        var updated = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Role updated", userMapper.toResponse(updated)));
    }

    // ── DELETE /admin/users/{id} ──────────────────────────────────────────────

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable UUID id) {
        log.info("ADMIN: disabling user={}", id);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User disabled", null));
    }

    // ── GET /admin/users/{id}/quota ───────────────────────────────────────────

    @GetMapping("/users/{id}/quota")
    public ResponseEntity<ApiResponse<QuotaResponse>> getUserQuota(@PathVariable UUID id) {
        // Ensure user exists first
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        ResourceQuota quota = quotaRepository.findOrCreateDefault(id);
        return ResponseEntity.ok(ApiResponse.success(toQuotaResponse(quota)));
    }

    // ── PUT /admin/users/{id}/quota ───────────────────────────────────────────

    @PutMapping("/users/{id}/quota")
    public ResponseEntity<ApiResponse<QuotaResponse>> updateUserQuota(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuotaRequest request) {

        log.info("ADMIN: updating quota for user={}: maxLabs={} maxCpu={} maxRam={} maxStorage={}",
                id, request.maxLabs(), request.maxCpu(), request.maxRamGb(), request.maxStorageGb());

        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        ResourceQuota quota = quotaRepository.findOrCreateDefault(id);
        quota.setMaxLabs(request.maxLabs());
        quota.setMaxCpu(request.maxCpu());
        quota.setMaxRamGb(request.maxRamGb());
        quota.setMaxStorageGb(request.maxStorageGb());
        ResourceQuota saved = quotaRepository.save(quota);

        return ResponseEntity.ok(ApiResponse.success("Quota updated", toQuotaResponse(saved)));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private QuotaResponse toQuotaResponse(ResourceQuota q) {
        return new QuotaResponse(
                q.getUserId(),
                q.getMaxLabs(),       q.getUsedLabs(),       q.getRemainingLabs(),
                q.getMaxCpu(),        q.getUsedCpu(),        q.getRemainingCpu(),
                q.getMaxRamGb(),      q.getUsedRamGb(),      q.getRemainingRamGb(),
                q.getMaxStorageGb(),  q.getUsedStorageGb(),  q.getRemainingStorage(),
                q.getUpdatedAt()
        );
    }
}
