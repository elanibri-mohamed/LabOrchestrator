package com.mnco.application.usecases;

import com.mnco.application.dto.request.CloneLabRequest;
import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.mapper.LabMapper;
import com.mnco.domain.entities.AuditLog.EventType;
import com.mnco.domain.entities.AuditLog.Result;
import com.mnco.domain.entities.Lab;
import com.mnco.domain.entities.LabStatus;
import com.mnco.domain.entities.ResourceQuota;
import com.mnco.domain.repository.LabRepository;
import com.mnco.domain.repository.ResourceQuotaRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.*;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import com.mnco.infrastructure.external.eveng.EveNgService;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgLabResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Complete lab lifecycle service with:
 *   - Full quota management (allocate on create, release on delete)
 *   - Audit logging on every operation (FR-LM-10)
 *   - Clone support (FR-LM-06)
 *   - Console URL retrieval (FR-LM-09)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabService implements LabUseCase {

    private final LabRepository labRepository;
    private final ResourceQuotaRepository quotaRepository;
    private final UserRepository userRepository;
    private final EveNgService eveNgService;
    private final LabMapper labMapper;
    private final AuditLogService auditLogService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabResponse createLab(CreateLabRequest request, UUID ownerId) {
        log.info("Creating lab '{}' for owner={}", request.name(), ownerId);
        String username = resolveUsername(ownerId);
        ResourceQuota quota = quotaRepository.findOrCreateDefault(ownerId);

        if (!quota.canAllocate(request.cpu(), request.ram(), request.storage())) {
            auditLogService.logLabEventFailure(EventType.LAB_CREATED, ownerId, username,
                    null, request.name(), "QUOTA_EXCEEDED");
            throw new QuotaExceededException(String.format(
                    "Quota exceeded. Remaining: labs=%d cpu=%d ram=%dGB storage=%dGB.",
                    quota.getRemainingLabs(), quota.getRemainingCpu(),
                    quota.getRemainingRamGb(), quota.getRemainingStorage()));
        }

        Lab lab = Lab.builder()
                .name(request.name()).description(request.description())
                .ownerId(ownerId).templateId(request.templateId())
                .cpuAllocated(request.cpu()).ramAllocated(request.ram())
                .storageAllocated(request.storage()).status(LabStatus.CREATING)
                .build();

        Lab saved = labRepository.save(lab);
        quota.allocate(request.cpu(), request.ram(), request.storage());
        quotaRepository.save(quota);

        try {
            EveNgLabResult result = eveNgService.createTopology(saved);
            saved.setEvengLabId(result.evengLabId());
            saved.setEvengNodeId(result.evengNodeId());
            saved.setStatus(LabStatus.STOPPED);
            Lab updated = labRepository.save(saved);
            auditLogService.logLabEvent(EventType.LAB_CREATED, ownerId, username,
                    updated.getId(), updated.getName(), Result.SUCCESS);
            log.info("Lab created: id={}", updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            log.error("EVE-NG creation failed for lab id={}: {}", saved.getId(), ex.getMessage());
            saved.markError();
            labRepository.save(saved);
            quota.release(request.cpu(), request.ram(), request.storage());
            quotaRepository.save(quota);
            auditLogService.logLabEventFailure(EventType.LAB_CREATED, ownerId, username,
                    saved.getId(), saved.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to create lab: " + ex.getMessage(), ex);
        }
    }

    // ── Clone (FR-LM-06) ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabResponse cloneLab(UUID sourceLabId, CloneLabRequest request, UUID requesterId) {
        log.info("Cloning lab id={} → '{}' for requester={}", sourceLabId, request.name(), requesterId);
        String username = resolveUsername(requesterId);

        Lab source = findLabAndCheckOwnership(sourceLabId, requesterId);

        if (!source.isStopped()) {
            throw new InvalidLabStateException(String.format(
                    "Lab '%s' must be STOPPED to clone (current: %s).",
                    source.getName(), source.getStatus()));
        }

        // Check quota for the clone — same resource profile as the source
        ResourceQuota quota = quotaRepository.findOrCreateDefault(requesterId);
        if (!quota.canAllocate(source.getCpuAllocated(), source.getRamAllocated(), source.getStorageAllocated())) {
            throw new QuotaExceededException("Quota exceeded — cannot create lab clone.");
        }

        // Create clone entity (PENDING until EVE-NG deep-copy succeeds)
        Lab clone = Lab.builder()
                .name(request.name())
                .description(request.description() != null
                        ? request.description() : "Clone of " + source.getName())
                .ownerId(requesterId)
                .templateId(source.getTemplateId())
                .cpuAllocated(source.getCpuAllocated())
                .ramAllocated(source.getRamAllocated())
                .storageAllocated(source.getStorageAllocated())
                .status(LabStatus.CREATING)
                .build();

        Lab savedClone = labRepository.save(clone);
        quota.allocate(source.getCpuAllocated(), source.getRamAllocated(), source.getStorageAllocated());
        quotaRepository.save(quota);

        try {
            EveNgCloneResult cloneResult = eveNgService.cloneLab(
                    source.getEvengLabId(),
                    request.name(),
                    savedClone.getId().toString());

            savedClone.setEvengLabId(cloneResult.clonedEvengLabId());
            savedClone.setStatus(LabStatus.STOPPED);
            Lab updated = labRepository.save(savedClone);

            auditLogService.logLabEvent(EventType.LAB_CLONED, requesterId, username,
                    updated.getId(), updated.getName(), Result.SUCCESS);

            log.info("Lab cloned: sourceId={} → cloneId={}", sourceLabId, updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            log.error("EVE-NG clone failed: {}", ex.getMessage());
            savedClone.markError();
            labRepository.save(savedClone);
            quota.release(source.getCpuAllocated(), source.getRamAllocated(), source.getStorageAllocated());
            quotaRepository.save(quota);
            auditLogService.logLabEventFailure(EventType.LAB_CLONED, requesterId, username,
                    sourceLabId, source.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to clone lab: " + ex.getMessage(), ex);
        }
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabResponse startLab(UUID labId, UUID requesterId) {
        String username = resolveUsername(requesterId);
        Lab lab = findLabAndCheckOwnership(labId, requesterId);
        if (!lab.isStartable()) {
            throw new InvalidLabStateException(String.format(
                    "Lab '%s' cannot be started from status '%s'.", lab.getName(), lab.getStatus()));
        }
        lab.setStatus(LabStatus.CREATING);
        labRepository.save(lab);
        try {
            eveNgService.startLab(lab.getEvengLabId());
            lab.markStarted();
            Lab updated = labRepository.save(lab);
            auditLogService.logLabEvent(EventType.LAB_STARTED, requesterId, username,
                    lab.getId(), lab.getName(), Result.SUCCESS);
            log.info("Lab started: id={}", updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            lab.markError();
            labRepository.save(lab);
            auditLogService.logLabEventFailure(EventType.LAB_STARTED, requesterId, username,
                    lab.getId(), lab.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to start lab: " + ex.getMessage(), ex);
        }
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabResponse stopLab(UUID labId, UUID requesterId) {
        String username = resolveUsername(requesterId);
        Lab lab = findLabAndCheckOwnership(labId, requesterId);
        if (!lab.isStoppable()) {
            throw new InvalidLabStateException(String.format(
                    "Lab '%s' is not RUNNING (current: %s).", lab.getName(), lab.getStatus()));
        }
        lab.setStatus(LabStatus.STOPPING);
        labRepository.save(lab);
        try {
            eveNgService.stopLab(lab.getEvengLabId());
            lab.markStopped();
            Lab updated = labRepository.save(lab);
            auditLogService.logLabEvent(EventType.LAB_STOPPED, requesterId, username,
                    lab.getId(), lab.getName(), Result.SUCCESS);
            log.info("Lab stopped: id={}", updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            lab.markError();
            labRepository.save(lab);
            auditLogService.logLabEventFailure(EventType.LAB_STOPPED, requesterId, username,
                    lab.getId(), lab.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to stop lab: " + ex.getMessage(), ex);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteLab(UUID labId, UUID requesterId) {
        String username = resolveUsername(requesterId);
        Lab lab = findLabAndCheckOwnership(labId, requesterId);
        if (!lab.isDeletable()) {
            throw new InvalidLabStateException(String.format(
                    "Lab '%s' must be STOPPED before deletion (current: %s).",
                    lab.getName(), lab.getStatus()));
        }
        lab.setStatus(LabStatus.DELETING);
        labRepository.save(lab);
        try {
            if (lab.getEvengLabId() != null) eveNgService.deleteLab(lab.getEvengLabId());
            lab.setStatus(LabStatus.DELETED);
            labRepository.save(lab);
            ResourceQuota quota = quotaRepository.findOrCreateDefault(requesterId);
            quota.release(lab.getCpuAllocated(), lab.getRamAllocated(), lab.getStorageAllocated());
            quotaRepository.save(quota);
            auditLogService.logLabEvent(EventType.LAB_DELETED, requesterId, username,
                    lab.getId(), lab.getName(), Result.SUCCESS);
            log.info("Lab deleted: id={}", labId);
        } catch (Exception ex) {
            lab.markError();
            labRepository.save(lab);
            auditLogService.logLabEventFailure(EventType.LAB_DELETED, requesterId, username,
                    lab.getId(), lab.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to delete lab: " + ex.getMessage(), ex);
        }
    }

    // ── Console (FR-LM-09) ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EveNgNodeConsoleInfo getNodeConsoleInfo(UUID labId, String nodeId, UUID requesterId) {
        Lab lab = findLabAndCheckOwnership(labId, requesterId);
        if (!lab.isRunning()) {
            throw new InvalidLabStateException(
                    "Lab must be RUNNING to access node console (current: " + lab.getStatus() + ")");
        }
        log.debug("Fetching console info: labId={}, nodeId={}", labId, nodeId);
        return eveNgService.getNodeConsoleInfo(lab.getEvengLabId(), nodeId);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LabResponse> getLabsByOwner(UUID ownerId) {
        return labRepository.findByOwnerId(ownerId).stream()
                .filter(l -> l.getStatus() != LabStatus.DELETED)
                .map(labMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabResponse getLabById(UUID labId, UUID requesterId, boolean isAdmin) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab not found: " + labId));
        if (!isAdmin && !lab.isOwnedBy(requesterId))
            throw new UnauthorizedException("Access denied: lab does not belong to you");
        return labMapper.toResponse(lab);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabResponse> getAllLabs() {
        return labRepository.findAll().stream().map(labMapper::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Lab findLabAndCheckOwnership(UUID labId, UUID requesterId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab not found: " + labId));
        if (!lab.isOwnedBy(requesterId))
            throw new UnauthorizedException("Access denied: lab does not belong to you");
        return lab;
    }

    private String resolveUsername(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getUsername()).orElse("unknown");
    }
}
