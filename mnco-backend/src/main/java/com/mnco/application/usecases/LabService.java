package com.mnco.application.usecases;

import com.mnco.application.dto.request.CloneLabRequest;
import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.mapper.LabMapper;
import com.mnco.domain.entities.AuditLog;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            auditLogService.logLabEventFailure(AuditLog.EventType.LAB_CREATED, ownerId, username,
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
            auditLogService.logLabEvent(AuditLog.EventType.LAB_CREATED, ownerId, username,
                    updated.getId(), updated.getName(), AuditLog.Result.SUCCESS);
            log.info("Lab created: id={}", updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            log.error("EVE-NG creation failed for lab id={}: {}", saved.getId(), ex.getMessage());
            saved.markError();
            labRepository.save(saved);
            quota.release(request.cpu(), request.ram(), request.storage());
            quotaRepository.save(quota);
            auditLogService.logLabEventFailure(AuditLog.EventType.LAB_CREATED, ownerId, username,
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

            auditLogService.logLabEvent(AuditLog.EventType.LAB_CLONED, requesterId, username,
                    updated.getId(), updated.getName(), AuditLog.Result.SUCCESS);

            log.info("Lab cloned: sourceId={} → cloneId={}", sourceLabId, updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            log.error("EVE-NG clone failed: {}", ex.getMessage());
            savedClone.markError();
            labRepository.save(savedClone);
            quota.release(source.getCpuAllocated(), source.getRamAllocated(), source.getStorageAllocated());
            quotaRepository.save(quota);
            auditLogService.logLabEventFailure(AuditLog.EventType.LAB_CLONED, requesterId, username,
                    sourceLabId, source.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to clone lab: " + ex.getMessage(), ex);
        }
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabResponse startLab(UUID labId, UUID requesterId, boolean isAdmin) {
        String username = resolveUsername(requesterId);
        Lab lab = findLabAndCheckAccess(labId, requesterId, isAdmin);
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
            auditLogService.logLabEvent(AuditLog.EventType.LAB_STARTED, requesterId, username,
                    lab.getId(), lab.getName(), AuditLog.Result.SUCCESS);
            log.info("Lab started: id={}", updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            lab.markError();
            labRepository.save(lab);
            auditLogService.logLabEventFailure(AuditLog.EventType.LAB_STARTED, requesterId, username,
                    lab.getId(), lab.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to start lab: " + ex.getMessage(), ex);
        }
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabResponse stopLab(UUID labId, UUID requesterId, boolean isAdmin) {
        String username = resolveUsername(requesterId);
        Lab lab = findLabAndCheckAccess(labId, requesterId, isAdmin);
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
            auditLogService.logLabEvent(AuditLog.EventType.LAB_STOPPED, requesterId, username,
                    lab.getId(), lab.getName(), AuditLog.Result.SUCCESS);
            log.info("Lab stopped: id={}", updated.getId());
            return labMapper.toResponse(updated);
        } catch (Exception ex) {
            lab.markError();
            labRepository.save(lab);
            auditLogService.logLabEventFailure(AuditLog.EventType.LAB_STOPPED, requesterId, username,
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
            auditLogService.logLabEvent(AuditLog.EventType.LAB_DELETED, requesterId, username,
                    lab.getId(), lab.getName(), AuditLog.Result.SUCCESS);
            log.info("Lab deleted: id={}", labId);
        } catch (Exception ex) {
            lab.markError();
            labRepository.save(lab);
            auditLogService.logLabEventFailure(AuditLog.EventType.LAB_DELETED, requesterId, username,
                    lab.getId(), lab.getName(), "EVENG_ERROR");
            throw new EveNgIntegrationException("Failed to delete lab: " + ex.getMessage(), ex);
        }
    }

    // ── Console (FR-LM-09) ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EveNgNodeConsoleInfo getNodeConsoleInfo(UUID labId, String nodeId, UUID requesterId, boolean isAdmin) {
        Lab lab = findLabAndCheckAccess(labId, requesterId, isAdmin);
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

    // ── Lab Discovery (Read-Only Mode) ────────────────────────────────────────

    @Override
    @Transactional
    public List<LabResponse> discoverLabsFromEveNg() {
        log.info("Starting lab discovery from EVE-NG server");
        List<LabResponse> discoveredLabs = new ArrayList<>();

        try {
            List<com.mnco.infrastructure.external.eveng.model.EveNgLabInfo> eveNgLabs = eveNgService.getAllLabs();
            log.info("Found {} labs in EVE-NG", eveNgLabs.size());

            for (com.mnco.infrastructure.external.eveng.model.EveNgLabInfo eveNgLab : eveNgLabs) {
                // Check if lab already exists in DB by evengLabId
                Optional<Lab> existingLab = labRepository.findByEvengLabId(eveNgLab.path());

                if (existingLab.isPresent()) {
                    log.debug("Lab already synced: {}", eveNgLab.path());
                    continue;
                }

                try {
                    // Parse resources from nodes
                    List<com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo> nodes =
                            eveNgService.getLabNodes(eveNgLab.path());

                    int totalCpu = 0;
                    int totalRam = 0;
                    int totalStorage = 0;

                    for (com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo node : nodes) {
                        totalCpu += node.getCpuCount();
                        totalRam += node.getRamGb();
                        totalStorage += node.getDiskGb();
                    }

                    // Ensure minimum allocations
                    if (totalCpu == 0) totalCpu = 1;
                    if (totalRam == 0) totalRam = 1;
                    if (totalStorage == 0) totalStorage = 1;

                    // Create local Lab record - assign to ADMIN user
                    // Get ADMIN user (assume ID known or find first admin)
                    UUID adminUserId = userRepository.findAll().stream()
                            .filter(u -> u.getRole().toString().equals("ADMIN"))
                            .map(u -> u.getId())
                            .findFirst()
                            .orElse(UUID.fromString("00000000-0000-0000-0000-000000000000"));

                    Lab newLab = Lab.builder()
                            .name(eveNgLab.getDisplayName())
                            .description(eveNgLab.description() != null ? eveNgLab.description() :
                                    "Lab discovered from EVE-NG - " + eveNgLab.path())
                            .ownerId(adminUserId)
                            .evengLabId(eveNgLab.path())
                            .cpuAllocated(totalCpu)
                            .ramAllocated(totalRam)
                            .storageAllocated(totalStorage)
                            .status(LabStatus.STOPPED)
                            .syncedFromEveNg(true)
                            .externalMetadata(String.format(
                                    "{\"nodeCount\":%d,\"discoveredAt\":\"%s\"}",
                                    nodes.size(), Instant.now()))
                            .build();

                    Lab saved = labRepository.save(newLab);
                    log.info("Lab synced from EVE-NG: id={} evengPath={}", saved.getId(), eveNgLab.path());
                    discoveredLabs.add(labMapper.toResponse(saved));

                    // Log discovery event
                    auditLogService.logLabEvent(
                            AuditLog.EventType.LAB_CREATED, adminUserId, "SYSTEM",
                            saved.getId(), saved.getName(), AuditLog.Result.SUCCESS);

                } catch (Exception ex) {
                    log.warn("Failed to sync lab {}: {}", eveNgLab.path(), ex.getMessage());
                    // Continue with next lab on error
                }
            }

            log.info("Lab discovery completed: {} labs synced", discoveredLabs.size());
            return discoveredLabs;

        } catch (Exception ex) {
            log.error("Lab discovery failed: {}", ex.getMessage(), ex);
            throw new EveNgIntegrationException("Failed to discover labs from EVE-NG: " + ex.getMessage(), ex);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Check if user has access to the lab:
     * - ADMIN: can access any lab
     * - Other roles: can only access labs they own (assigned to them)
     */
    private Lab findLabAndCheckAccess(UUID labId, UUID requesterId, boolean isAdmin) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab not found: " + labId));
        if (!isAdmin && !lab.isOwnedBy(requesterId))
            throw new UnauthorizedException("Access denied: lab does not belong to you");
        return lab;
    }

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
