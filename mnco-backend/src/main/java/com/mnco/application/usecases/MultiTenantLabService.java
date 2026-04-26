package com.mnco.application.usecases;

import com.mnco.application.dto.response.*;
import com.mnco.application.mapper.EvengTemplateMapper;
import com.mnco.application.mapper.LabInstanceMapper;
import com.mnco.application.security.InstanceGuard;
import com.mnco.domain.entities.*;
import com.mnco.domain.repository.*;
import com.mnco.exception.custom.*;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import com.mnco.infrastructure.external.eveng.EveNgService;
import com.mnco.infrastructure.external.eveng.model.EveNgCloneResult;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeStatus;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of the multi-tenant lab lifecycle per reference architecture.
 * Manages EVE-NG templates, assignments, instances, and progress monitoring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MultiTenantLabService {

    private final EvengTemplateRepository templateRepository;
    private final LabAssignmentRepository assignmentRepository;
    private final LabInstanceRepository instanceRepository;
    private final ConsoleAccessLogRepository consoleLogRepository;
    private final UserRepository userRepository;
    private final EveNgService eveNgService;
    private final InstanceGuard guard;
    private final EvengTemplateMapper templateMapper;
    private final LabInstanceMapper instanceMapper;
    private final AuditLogService auditLog;
    private final MeterRegistry meterRegistry;

    // ── SYNC TEMPLATES FROM EVE-NG ─────────────────────────────────────────────

    @Transactional
    public SyncResultResponse syncTemplates(UUID adminId) {
        guard.ensureAdmin(adminId, userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"))
                .getRole());

        log.info("Starting EVE-NG template sync by admin={}", adminId);
        List<com.mnco.infrastructure.external.eveng.model.EveNgLabInfo> eveLabs;
        try {
            eveLabs = eveNgService.getAllLabs();
        } catch (Exception ex) {
            log.error("EVE-NG sync failed: {}", ex.getMessage(), ex);
            throw new EveNgIntegrationException("Failed to fetch labs from EVE-NG", ex);
        }

        Set<String> currentPaths = eveLabs.stream()
                .map(com.mnco.infrastructure.external.eveng.model.EveNgLabInfo::path)
                .collect(Collectors.toSet());

        int created = 0, updated = 0;

        // Upsert each lab from EVE-NG
        for (com.mnco.infrastructure.external.eveng.model.EveNgLabInfo eveLab : eveLabs) {
            Optional<EvengTemplate> existingOpt = templateRepository.findByEvengTemplatePath(eveLab.path());
            if (existingOpt.isEmpty()) {
                EvengTemplate template = EvengTemplate.builder()
                        .name(eveLab.getDisplayName())
                        .evengTemplatePath(eveLab.path())
                        .status(LabTemplateStatus.ACTIVE)
                        .lastSyncedAt(Instant.now())
                        .description(eveLab.description())
                        .build();
                templateRepository.save(template);
                created++;
                log.debug("Synced new template: {}", eveLab.path());
            } else {
                EvengTemplate existing = existingOpt.get();
                existing.setName(eveLab.getDisplayName());
                existing.setLastSyncedAt(Instant.now());
                if (existing.getStatus() == LabTemplateStatus.REMOVED) {
                    existing.setStatus(LabTemplateStatus.ACTIVE);
                    log.info("Restored previously removed template: {}", eveLab.path());
                }
                templateRepository.save(existing);
                updated++;
            }
        }

        // Soft-delete templates no longer in EVE-NG
        int removed = 0;
        List<EvengTemplate> allActive = templateRepository.findAllActive();
        for (EvengTemplate t : allActive) {
            if (!currentPaths.contains(t.getEvengTemplatePath())) {
                t.setStatus(LabTemplateStatus.REMOVED);
                templateRepository.save(t);
                removed++;
                log.info("Marked template as REMOVED (no longer in EVE-NG): {}", t.getEvengTemplatePath());
            }
        }

        log.info("Sync complete: created={}, updated={}, removed={}", created, updated, removed);
        // Metrics
        meterRegistry.counter("eveng.sync", "outcome", "created").increment(created);
        meterRegistry.counter("eveng.sync", "outcome", "updated").increment(updated);
        meterRegistry.counter("eveng.sync", "outcome", "removed").increment(removed);
        auditLog.logLabEvent(AuditLog.EventType.LAB_CREATED, adminId, "SYSTEM", null, "Sync Templates",
                AuditLog.Result.SUCCESS);
        return new SyncResultResponse(created, updated, removed);
    }

    // ── LIST ACCESSIBLE TEMPLATES ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EvengTemplateResponse> getAccessibleTemplates(UUID userId) {
        List<LabAssignment> assignments = assignmentRepository.findByUserId(userId);
        return assignments.stream()
                .map(a -> templateRepository.findById(a.getTemplateId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(t -> t.getStatus() == LabTemplateStatus.ACTIVE)
                .map(templateMapper::toResponse)
                .sorted(Comparator.comparing(EvengTemplateResponse::name))
                .toList();
    }

    // ── ASSIGNMENTS ────────────────────────────────────────────────────────────

    @Transactional
    public void assignToTeacher(UUID templateId, UUID teacherId, UUID adminId) {
        guard.ensureAdmin(adminId, userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"))
                .getRole());
        guard.ensureTemplateActive(templateId);

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + teacherId));
        if (!teacher.isTeacher()) {
            throw new AccessDeniedException("Target user is not a teacher");
        }

        if (!assignmentRepository.existsByTemplateIdAndUserId(templateId, teacherId)) {
            LabAssignment assignment = LabAssignment.builder()
                    .templateId(templateId)
                    .userId(teacherId)
                    .assignedBy(adminId)
                    .build();
            assignmentRepository.save(assignment);
            log.info("Assigned template {} to teacher {} by admin {}", templateId, teacherId, adminId);
        } else {
            log.debug("Assignment already exists: template {} -> teacher {}", templateId, teacherId);
        }
    }

    @Transactional
    public void assignToStudent(UUID templateId, UUID studentId, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        guard.guardAssignStudent(templateId, teacherId, teacher.getRole(), studentId);

        if (!assignmentRepository.existsByTemplateIdAndUserId(templateId, studentId)) {
            LabAssignment assignment = LabAssignment.builder()
                    .templateId(templateId)
                    .userId(studentId)
                    .assignedBy(teacherId)
                    .build();
            assignmentRepository.save(assignment);
            log.info("Assigned template {} to student {} by teacher {}", templateId, studentId, teacherId);
        } else {
            log.debug("Assignment already exists: template {} -> student {}", templateId, studentId);
        }
    }

    @Transactional
    public void revokeAssignment(UUID templateId, UUID targetUserId, UUID requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LabAssignment assignment = assignmentRepository.findByTemplateIdAndUserId(templateId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (requester.isAdmin()) {
            // admin can revoke any
        } else if (requester.isTeacher()) {
            // teacher can only revoke students they assigned
            if (!assignment.getAssignedBy().equals(requesterId)) {
                throw new AccessDeniedException("You did not make this assignment");
            }
        } else {
            throw new AccessDeniedException("Insufficient privileges to revoke assignment");
        }

        assignmentRepository.deleteByTemplateIdAndUserId(templateId, targetUserId);
        log.info("Revoked assignment: template {} from user {} by {}", templateId, targetUserId, requesterId);
    }

    // ── INSTANCE LIFECYCLE ─────────────────────────────────────────────────────

    @Transactional
    public LabInstanceResponse startInstance(UUID templateId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        guard.guardTemplateAccess(templateId, userId, user.getRole());

        EvengTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        // Find or create instance
        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseGet(() -> {
                    UUID instanceId = UUID.randomUUID();
                    // Copy template to new instance file via EVE-NG clone (export+import)
                    EveNgCloneResult cloneResult = eveNgService.cloneLab(
                            template.getEvengTemplatePath(),
                            "inst-" + instanceId,
                            instanceId.toString()
                    );
                    LabInstance newInst = LabInstance.builder()
                            .id(instanceId)
                            .templateId(templateId)
                            .userId(userId)
                            .evengInstancePath(cloneResult.clonedEvengLabId())
                            .status(InstanceStatus.STOPPED)
                            .build();
                    return instanceRepository.save(newInst);
                });

        if (instance.getStatus() == InstanceStatus.RUNNING) {
            log.info("Instance already running: id={}", instance.getId());
            return instanceMapper.toResponse(instance);
        }

        // Start via EVE-NG
        try {
            eveNgService.startLab(instance.getEvengInstancePath());
        } catch (Exception ex) {
            log.error("Failed to start instance {}: {}", instance.getId(), ex.getMessage(), ex);
            throw new EveNgIntegrationException("Failed to start lab instance", ex);
        }
        instance.markRunning(); // sets status and startedAt
        LabInstance saved = instanceRepository.save(instance);

        auditLog.logLabEvent(AuditLog.EventType.LAB_STARTED,
                userId, user.getUsername(), saved.getId(), template.getName(),
                AuditLog.Result.SUCCESS);
        meterRegistry.counter("lab.instance.start").increment();
        log.info("Instance started: id={}, userId={}", saved.getId(), userId);
        return instanceMapper.toResponse(saved);
    }

    @Transactional
    public LabInstanceResponse stopOwnInstance(UUID templateId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isTeacher()) {
            throw new AccessDeniedException("Only teachers can stop their own labs");
        }
        guard.ensureTemplateActive(templateId);
        guard.ensureUserAssigned(templateId, userId);

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found. Start the lab first."));

        if (instance.getStatus() != InstanceStatus.RUNNING) {
            throw new InvalidLabStateException("Instance is not running (current: " + instance.getStatus() + ")");
        }

        try {
            eveNgService.stopLab(instance.getEvengInstancePath());
        } catch (Exception ex) {
            log.error("Failed to stop instance {}: {}", instance.getId(), ex.getMessage(), ex);
            throw new EveNgIntegrationException("Failed to stop lab instance", ex);
        }
        instance.markStopped();
        LabInstance saved = instanceRepository.save(instance);
        auditLog.logLabEvent(AuditLog.EventType.LAB_STOPPED,
                userId, user.getUsername(), saved.getId(), templateId.toString(),
                AuditLog.Result.SUCCESS);
        meterRegistry.counter("lab.instance.stop").increment();
        log.info("Instance stopped: id={}", saved.getId());
        return instanceMapper.toResponse(saved);
    }

    @Transactional
    public LabInstanceResponse stopStudentInstance(UUID templateId, UUID studentId, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        guard.guardTeacherManageStudentInstance(templateId, teacherId, teacher.getRole(), studentId);

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student instance not found"));
        if (instance.getStatus() != InstanceStatus.RUNNING) {
            throw new InvalidLabStateException("Student instance is not running");
        }

        try {
            eveNgService.stopLab(instance.getEvengInstancePath());
        } catch (Exception ex) {
            log.error("Failed to stop student instance {}: {}", instance.getId(), ex.getMessage(), ex);
            throw new EveNgIntegrationException("Failed to stop student lab instance", ex);
        }
        instance.markStopped();
        LabInstance saved = instanceRepository.save(instance);
        auditLog.logLabEvent(AuditLog.EventType.LAB_STOPPED,
                teacherId, teacher.getUsername(), saved.getId(), templateId.toString(),
                AuditLog.Result.SUCCESS);
        meterRegistry.counter("lab.instance.stop").increment();
        log.info("Student instance stopped: id={} by teacher {}", saved.getId(), teacherId);
        return instanceMapper.toResponse(saved);
    }

    @Transactional
    public LabInstanceResponse resetInstance(UUID templateId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        guard.guardTemplateAccess(templateId, userId, user.getRole());

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found"));

        // Stop if running
        if (instance.getStatus() == InstanceStatus.RUNNING) {
            try {
                eveNgService.stopLab(instance.getEvengInstancePath());
            } catch (Exception ex) {
                log.warn("Error stopping instance before reset: {}", ex.getMessage());
            }
        }

        // Delete old instance
        try {
            eveNgService.deleteLab(instance.getEvengInstancePath());
        } catch (Exception ex) {
            log.warn("Error deleting old instance file: {}", ex.getMessage());
        }

        // Re-copy template
        EvengTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        EveNgCloneResult result;
        try {
            result = eveNgService.cloneLab(
                    template.getEvengTemplatePath(),
                    "inst-" + instance.getId(),
                    instance.getId().toString()
            );
        } catch (Exception ex) {
            log.error("Failed to clone template for instance reset: {}", instance.getId(), ex);
            throw new EveNgIntegrationException("Failed to recreate instance from template", ex);
        }

        instance.setEvengInstancePath(result.clonedEvengLabId());
        instance.reset();
        LabInstance saved = instanceRepository.save(instance);
        auditLog.logLabEvent(AuditLog.EventType.LAB_CLONED,
                userId, user.getUsername(), saved.getId(), template.getName(),
                AuditLog.Result.SUCCESS);
        meterRegistry.counter("lab.instance.reset").increment();
        log.info("Instance reset: id={}", saved.getId());
        return instanceMapper.toResponse(saved);
    }

    @Transactional
    public LabInstanceResponse resetStudentInstance(UUID templateId, UUID studentId, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        guard.guardTeacherManageStudentInstance(templateId, teacherId, teacher.getRole(), studentId);

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student instance not found"));

        if (instance.getStatus() == InstanceStatus.RUNNING) {
            try {
                eveNgService.stopLab(instance.getEvengInstancePath());
            } catch (Exception ex) {
                log.warn("Error stopping student instance before reset: {}", ex.getMessage());
            }
        }
        try {
            eveNgService.deleteLab(instance.getEvengInstancePath());
        } catch (Exception ex) {
            log.warn("Error deleting student instance file: {}", ex.getMessage());
        }

        EvengTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        EveNgCloneResult result;
        try {
            result = eveNgService.cloneLab(
                    template.getEvengTemplatePath(),
                    "inst-" + instance.getId(),
                    instance.getId().toString()
            );
        } catch (Exception ex) {
            log.error("Failed to clone template for student instance reset: {}", instance.getId(), ex);
            throw new EveNgIntegrationException("Failed to recreate student instance", ex);
        }

        instance.setEvengInstancePath(result.clonedEvengLabId());
        instance.reset();
        LabInstance saved = instanceRepository.save(instance);
        auditLog.logLabEvent(AuditLog.EventType.LAB_CLONED,
                teacherId, teacher.getUsername(), saved.getId(), template.getName(),
                AuditLog.Result.SUCCESS);
        meterRegistry.counter("lab.instance.reset").increment();
        log.info("Student instance reset: id={} by teacher {}", saved.getId(), teacherId);
        return instanceMapper.toResponse(saved);
    }

    // ── CONSOLE ACCESS ─────────────────────────────────────────────────────────

    @Transactional
    public EveNgNodeConsoleInfo getConsoleUrl(UUID templateId, UUID userId, String nodeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        guard.guardTemplateAccess(templateId, userId, user.getRole());

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found. Start the lab first."));

        if (instance.getStatus() != InstanceStatus.RUNNING) {
            throw new InvalidLabStateException("Lab must be RUNNING to access console");
        }

        EveNgNodeConsoleInfo consoleInfo;
        try {
            consoleInfo = eveNgService.getNodeConsoleInfo(instance.getEvengInstancePath(), nodeId);
        } catch (Exception ex) {
            log.error("Failed to get console info for instance={}, nodeId={}", instance.getId(), nodeId, ex);
            throw new EveNgIntegrationException("Failed to retrieve console information", ex);
        }

        // Log access
        ConsoleAccessLog accessLog = ConsoleAccessLog.builder()
                .userId(userId)
                .instanceId(instance.getId())
                .nodeId(nodeId)
                .accessedAt(Instant.now())
                .build();
        consoleLogRepository.save(accessLog);

        meterRegistry.counter("lab.console.access").increment();
        return consoleInfo;
    }

    // ── PROGRESS MONITORING (PARALLEL) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentProgressReport> getProgress(UUID templateId, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        guard.ensureTeacher(teacherId, teacher.getRole());
        guard.ensureTemplateActive(templateId);
        guard.ensureUserAssigned(templateId, teacherId);

        List<LabAssignment> studentAssignments = assignmentRepository.findByTemplateIdAndAssignedBy(templateId, teacherId);
        if (studentAssignments.isEmpty()) {
            return List.of();
        }

        // Parallel fetch of student reports
        List<CompletableFuture<StudentProgressReport>> futures = studentAssignments.stream()
                .map(assign -> CompletableFuture.supplyAsync(() -> {
                    UUID studentId = assign.getUserId();
                    User student = userRepository.findById(studentId).orElse(null);
                    String studentName = student != null ? student.getUsername() : "unknown";
                    return buildStudentReport(templateId, studentId, studentName);
                }))
                .toList();

        // Wait for all and collect
        List<StudentProgressReport> reports = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        meterRegistry.counter("lab.progress.request").increment();
        return reports;
    }

    private StudentProgressReport buildStudentReport(UUID templateId, UUID studentId, String studentName) {
        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, studentId)
                .orElse(null);

        StudentProgressReport.InstanceLifecycle lifecycle;
        Instant startedAt = null;
        Map<String, String> nodeStatuses = Map.of();

        if (instance == null) {
            lifecycle = StudentProgressReport.InstanceLifecycle.NOT_CREATED;
        } else {
            switch (instance.getStatus()) {
                case RUNNING -> {
                    lifecycle = StudentProgressReport.InstanceLifecycle.RUNNING;
                    startedAt = instance.getStartedAt();
                    try {
                        List<EveNgNodeStatus> nodes = eveNgService.getLabNodeStatuses(instance.getEvengInstancePath());
                        nodeStatuses = nodes.stream()
                                .collect(Collectors.toMap(
                                        EveNgNodeStatus::id,
                                        ns -> ns.status() == 2 ? "RUNNING" : "STOPPED"));
                    } catch (Exception ex) {
                        log.warn("Could not fetch node statuses for instance {}: {}", instance.getId(), ex.getMessage());
                        nodeStatuses = Map.of();
                    }
                }
                case STOPPED -> {
                    lifecycle = StudentProgressReport.InstanceLifecycle.STOPPED;
                    startedAt = instance.getStartedAt();
                }
                case ERROR -> {
                    lifecycle = StudentProgressReport.InstanceLifecycle.ERROR;
                }
                default -> lifecycle = StudentProgressReport.InstanceLifecycle.NOT_CREATED;
            }
        }

        // Last console access
        Instant lastConnected = null;
        List<String> accessedNodes = List.of();
        if (instance != null) {
            var lastLogOpt = consoleLogRepository
                    .findTopByInstanceIdAndUserIdOrderByAccessedAtDesc(instance.getId(), studentId);
            lastConnected = lastLogOpt.map(ConsoleAccessLog::getAccessedAt).orElse(null);
            accessedNodes = consoleLogRepository.findDistinctNodeIdByInstanceIdAndUserId(instance.getId(), studentId);
        }

        return new StudentProgressReport(
                studentId,
                studentName,
                lifecycle,
                startedAt,
                nodeStatuses,
                lastConnected,
                accessedNodes
        );
    }

    @Transactional(readOnly = true)
    public List<LabInstanceResponse> listStudentInstances(UUID templateId, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        guard.ensureTeacher(teacherId, teacher.getRole());
        guard.ensureTemplateActive(templateId);
        guard.ensureUserAssigned(templateId, teacherId);

        List<LabAssignment> assignments = assignmentRepository.findByTemplateIdAndAssignedBy(templateId, teacherId);
        return assignments.stream()
                .map(a -> instanceRepository.findByTemplateIdAndUserId(templateId, a.getUserId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(instanceMapper::toResponse)
                .toList();
    }
}
