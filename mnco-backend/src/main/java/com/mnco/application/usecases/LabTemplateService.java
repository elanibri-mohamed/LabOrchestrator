package com.mnco.application.usecases;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.mapper.LabTemplateMapper;
import com.mnco.domain.entities.AuditLog;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.domain.entities.LabTemplateStatus;
import com.mnco.domain.entities.User;
import com.mnco.domain.repository.LabAssignmentRepository;
import com.mnco.domain.repository.LabTemplateRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.EveNgIntegrationException;
import com.mnco.exception.custom.UnauthorizedException;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.infrastructure.external.eveng.EveNgService;
import com.mnco.infrastructure.external.eveng.model.EveNgLabInfo;
import com.mnco.infrastructure.external.eveng.model.EveNgNodeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabTemplateService implements LabTemplateUseCase {

    private final LabTemplateRepository templateRepository;
    private final LabAssignmentRepository assignmentRepository;
    private final EveNgService eveNgService;
    private final LabTemplateMapper templateMapper;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LabResponse> getAllTemplates() {
    User currentUser = getCurrentUser();

    if (currentUser.isAdmin()) {
        return templateRepository.findAllByStatus(LabTemplateStatus.ACTIVE).stream()
            .map(templateMapper::toResponse)
            .collect(Collectors.toList());
    }

    Set<UUID> assignedTemplateIds = assignmentRepository.findAllByUserId(currentUser.getId()).stream()
        .map(assignment -> assignment.getTemplateId())
        .collect(Collectors.toSet());

    return templateRepository.findAllByStatus(LabTemplateStatus.ACTIVE).stream()
        .filter(template -> assignedTemplateIds.contains(template.getId()))
                .map(templateMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LabResponse getTemplateById(UUID id) {
    LabTemplate template = templateRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));

    User currentUser = getCurrentUser();
    if (!currentUser.isAdmin() && !assignmentRepository.existsByTemplateIdAndUserId(id, currentUser.getId())) {
        throw new UnauthorizedException("You are not assigned to this lab template");
    }

    return templateMapper.toResponse(template);
    }

    @Override
    @Transactional
    public LabResponse updateTemplateDescription(UUID id, String description) {
        User currentUser = getCurrentUser();
        LabTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));

        if (!currentUser.isAdmin() && !assignmentRepository.existsByTemplateIdAndUserId(id, currentUser.getId())) {
            throw new UnauthorizedException("You are not assigned to this lab template");
        }

        String normalizedDescription = description == null ? null : description.trim();
        if (normalizedDescription != null && normalizedDescription.isEmpty()) {
            normalizedDescription = null;
        }

        template.setDescription(normalizedDescription);
        template.setLastSyncedAt(template.getLastSyncedAt());
        LabTemplate saved = templateRepository.save(template);
        return templateMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<LabResponse> discoverTemplatesFromEveNg() {
        log.info("Starting template discovery from EVE-NG /templates directory");
        List<LabResponse> syncedTemplates = new ArrayList<>();

        try {
            // In our multi-tenant plan, templates are in /opt/unetlab/labs/templates/
            // Instances (deployed labs) are in /instances/ and should NOT be synced as templates
            List<EveNgLabInfo> eveNgLabs = eveNgService.getAllLabs();
            
            // Filter to only include labs that are NOT in /instances/ directory
            // /instances/ contains deployed lab copies, not template definitions
            List<String> activePaths = new ArrayList<>();

            for (EveNgLabInfo eveNgLab : eveNgLabs) {
                // Skip labs in /instances/ folder (these are lab instances, not templates)
                if (eveNgLab.path().startsWith("/instances/")) {
                    log.debug("Skipping lab instance (not a template): {}", eveNgLab.path());
                    continue;
                }
                
                activePaths.add(eveNgLab.path());
                Optional<LabTemplate> existing = templateRepository.findByEveTemplatePath(eveNgLab.path());

                if (existing.isEmpty()) {
                    // New template found
                    List<EveNgNodeInfo> nodes = eveNgService.getLabNodes(eveNgLab.path());
                    int cpu = nodes.stream().mapToInt(EveNgNodeInfo::getCpuCount).sum();
                    int ram = nodes.stream().mapToInt(EveNgNodeInfo::getRamGb).sum();
                    int storage = nodes.stream().mapToInt(EveNgNodeInfo::getDiskGb).sum();

                    // Handle duplicate template names: if a template with the same name already exists
                    // but with a different path, append the lab instance ID to make the name unique
                    String templateName = eveNgLab.getDisplayName();
                    String uniqueName = templateName;
                    int counter = 1;
                    while (templateRepository.findByName(uniqueName).isPresent()) {
                        // Extract instance UUID from path if available (e.g., /instances/{uuid}/lab.unl)
                        String pathPart = eveNgLab.path().contains("/instances/") 
                            ? eveNgLab.path().substring(0, Math.min(eveNgLab.path().length(), eveNgLab.path().lastIndexOf("/")))
                            : "";
                        uniqueName = templateName + (counter > 1 ? " (" + counter + ")" : " (instance)");
                        counter++;
                    }

                    LabTemplate template = LabTemplate.builder()
                            .name(uniqueName)
                            .description(eveNgLab.description() != null ? eveNgLab.description() : "Synced from EVE-NG")
                            .eveTemplatePath(eveNgLab.path())
                            .status(LabTemplateStatus.ACTIVE)
                            .cpuAllocated(Math.max(cpu, 1))
                            .ramAllocated(Math.max(ram, 1))
                            .storageAllocated(Math.max(storage, 1))
                            .lastSyncedAt(Instant.now())
                            .build();

                    LabTemplate saved = templateRepository.save(template);
                    syncedTemplates.add(templateMapper.toResponse(saved));
                    log.info("New template synced: {} (path={})", saved.getName(), saved.getEveTemplatePath());
                } else {
                    // Update existing template
                    LabTemplate template = existing.get();
                    template.setName(eveNgLab.getDisplayName());
                    template.setStatus(LabTemplateStatus.ACTIVE);
                    template.setLastSyncedAt(Instant.now());
                    templateRepository.save(template);
                }
            }

            // Soft delete: Mark templates as REMOVED if they are no longer in EVE-NG
            List<LabTemplate> allActive = templateRepository.findAllByStatus(LabTemplateStatus.ACTIVE);
            for (LabTemplate t : allActive) {
                if (!activePaths.contains(t.getEveTemplatePath())) {
                    t.setStatus(LabTemplateStatus.REMOVED);
                    templateRepository.save(t);
                    log.info("Template marked as REMOVED: {}", t.getEveTemplatePath());
                }
            }

            return syncedTemplates;

        } catch (Exception ex) {
            log.error("Template discovery failed: {}", ex.getMessage(), ex);
            throw new EveNgIntegrationException("Failed to sync templates: " + ex.getMessage(), ex);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Authentication required");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
