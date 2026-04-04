package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.QuotaResponse;
import com.mnco.domain.entities.ResourceQuota;
import com.mnco.domain.repository.ResourceQuotaRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Allows any authenticated user to check their own resource quota.
 *
 * GET /quota/me  — returns current usage and remaining quota
 */
@RestController
@RequestMapping("/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final ResourceQuotaRepository quotaRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<QuotaResponse>> getMyQuota(
            @AuthenticationPrincipal UserDetails userDetails) {

        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ResourceQuota quota = quotaRepository.findOrCreateDefault(user.getId());

        QuotaResponse response = new QuotaResponse(
                quota.getUserId(),
                quota.getMaxLabs(),       quota.getUsedLabs(),       quota.getRemainingLabs(),
                quota.getMaxCpu(),        quota.getUsedCpu(),         quota.getRemainingCpu(),
                quota.getMaxRamGb(),      quota.getUsedRamGb(),      quota.getRemainingRamGb(),
                quota.getMaxStorageGb(),  quota.getUsedStorageGb(),  quota.getRemainingStorage(),
                quota.getUpdatedAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
