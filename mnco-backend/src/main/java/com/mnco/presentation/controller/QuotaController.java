package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.QuotaResponse;
import com.mnco.domain.entities.ResourceQuota;
import com.mnco.domain.repository.ResourceQuotaRepository;
import com.mnco.security.service.UserDetailsServiceImpl.MncoUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/quota", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class QuotaController {

    private final ResourceQuotaRepository quotaRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<QuotaResponse>> getMyQuota(
            @AuthenticationPrincipal MncoUserDetails principal) {
        ResourceQuota q = quotaRepository.findOrCreateDefault(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(new QuotaResponse(
                q.getUserId(),
                q.getMaxLabs(),      q.getUsedLabs(),      q.getRemainingLabs(),
                q.getMaxCpu(),       q.getUsedCpu(),       q.getRemainingCpu(),
                q.getMaxRamGb(),     q.getUsedRamGb(),     q.getRemainingRamGb(),
                q.getMaxStorageGb(), q.getUsedStorageGb(), q.getRemainingStorage(),
                q.getUpdatedAt())));
    }
}