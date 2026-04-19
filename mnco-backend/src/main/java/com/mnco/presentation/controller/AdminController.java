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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ResourceQuotaRepository quotaRepository;
    private final UserMapper userMapper;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(userMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponse(user)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable UUID id,
            @RequestParam UserRole role) {
        log.info("ADMIN: changing role of user={} to {}", id, role);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setRole(role);
        return ResponseEntity.ok(ApiResponse.success("Role updated",
                userMapper.toResponse(userRepository.save(user))));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> disableUser(@PathVariable UUID id) {
        log.info("ADMIN: disabling user={}", id);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setEnabled(false);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/quota")
    public ResponseEntity<ApiResponse<QuotaResponse>> getUserQuota(@PathVariable UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(toQuotaResponse(
                quotaRepository.findOrCreateDefault(id))));
    }

    @PutMapping("/users/{id}/quota")
    public ResponseEntity<ApiResponse<QuotaResponse>> updateUserQuota(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuotaRequest request) {
        log.info("ADMIN: updating quota for user={}", id);
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        ResourceQuota quota = quotaRepository.findOrCreateDefault(id);
        quota.setMaxLabs(request.maxLabs());
        quota.setMaxCpu(request.maxCpu());
        quota.setMaxRamGb(request.maxRamGb());
        quota.setMaxStorageGb(request.maxStorageGb());
        return ResponseEntity.ok(ApiResponse.success("Quota updated",
                toQuotaResponse(quotaRepository.save(quota))));
    }

    private QuotaResponse toQuotaResponse(ResourceQuota q) {
        return new QuotaResponse(
                q.getUserId(),
                q.getMaxLabs(),      q.getUsedLabs(),      q.getRemainingLabs(),
                q.getMaxCpu(),       q.getUsedCpu(),       q.getRemainingCpu(),
                q.getMaxRamGb(),     q.getUsedRamGb(),     q.getRemainingRamGb(),
                q.getMaxStorageGb(), q.getUsedStorageGb(), q.getRemainingStorage(),
                q.getUpdatedAt());
    }
}