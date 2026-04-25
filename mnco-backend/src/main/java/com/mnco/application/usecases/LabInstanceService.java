package com.mnco.application.usecases;

import com.mnco.application.dto.response.LabResponse;
import com.mnco.application.mapper.LabInstanceMapper;
import com.mnco.application.mapper.LabTemplateMapper;
import com.mnco.domain.entities.*;
import com.mnco.domain.repository.LabAssignmentRepository;
import com.mnco.domain.repository.LabInstanceRepository;
import com.mnco.domain.repository.LabTemplateRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.InvalidLabStateException;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.exception.custom.UnauthorizedException;
import com.mnco.infrastructure.external.eveng.EveNgNodeConsoleInfo;
import com.mnco.infrastructure.external.eveng.EveNgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabInstanceService implements LabInstanceUseCase {

    private final LabInstanceRepository instanceRepository;
    private final LabTemplateRepository templateRepository;
    private final LabAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EveNgService eveNgService;
    private final LabTemplateMapper templateMapper;
    private final LabInstanceMapper instanceMapper;

    @Override
    @Transactional
    public LabResponse startInstance(UUID templateId, UUID userId) {
        log.info("User {} starting instance of template {}", userId, templateId);

        // Guard 3: Assignment check
        if (!assignmentRepository.existsByTemplateIdAndUserId(templateId, userId)) {
            throw new UnauthorizedException("You are not assigned to this lab template");
        }

        LabTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseGet(() -> createNewInstance(template, userId));

        try {
            eveNgService.startLab(instance.getEveInstancePath());
            instance.setStatus(InstanceStatus.RUNNING);
            instance.setStartedAt(Instant.now());
            LabInstance saved = instanceRepository.save(instance);
            return toResponse(template, saved);
        } catch (Exception ex) {
            instance.setStatus(InstanceStatus.ERROR);
            instanceRepository.save(instance);
            throw ex;
        }
    }

    @Override
    @Transactional
    public LabResponse stopInstance(UUID templateId, UUID userId) {
        log.info("User {} stopping instance of template {}", userId, templateId);

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No running instance found for this lab"));

        try {
            eveNgService.stopLab(instance.getEveInstancePath());
            instance.setStatus(InstanceStatus.STOPPED);
            instance.setStoppedAt(Instant.now());
            LabInstance saved = instanceRepository.save(instance);
            LabTemplate template = templateRepository.findById(templateId).get();
            return toResponse(template, saved);
        } catch (Exception ex) {
            instance.setStatus(InstanceStatus.ERROR);
            instanceRepository.save(instance);
            throw ex;
        }
    }

    @Override
    @Transactional
    public LabResponse resetInstance(UUID templateId, UUID userId) {
        log.info("User {} resetting instance of template {}", userId, templateId);

        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found"));

        if (instance.isRunning()) {
            eveNgService.stopLab(instance.getEveInstancePath());
        }

        // Delete and Re-clone
        eveNgService.deleteLab(instance.getEveInstancePath());
        LabTemplate template = templateRepository.findById(templateId).get();
        eveNgService.copyLab(template.getEveTemplatePath(), instance.getEveInstancePath());

        instance.setStatus(InstanceStatus.STOPPED);
        instance.setStartedAt(null);
        instance.setStoppedAt(null);
        LabInstance saved = instanceRepository.save(instance);
        
        return toResponse(template, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabResponse> getMyInstances(UUID userId) {
        // List all assigned templates and their instance status
        return assignmentRepository.findAllByUserId(userId).stream()
                .map(assignment -> {
                    LabTemplate template = templateRepository.findById(assignment.getTemplateId()).get();
                    LabInstance instance = instanceRepository.findByTemplateIdAndUserId(template.getId(), userId)
                            .orElse(null);
                    return toResponse(template, instance);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EveNgNodeConsoleInfo getNodeConsoleInfo(UUID templateId, String nodeId, UUID userId) {
        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found"));

        if (!instance.isRunning()) {
            throw new InvalidLabStateException("Lab instance is not running");
        }

        return eveNgService.getNodeConsoleInfo(instance.getEveInstancePath(), nodeId);
    }

    @Override
    public Map<String, Object> getInstanceNodes(UUID templateId, UUID userId) {
        LabInstance instance = instanceRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Instance not found"));

        if (!instance.isRunning()) {
            throw new InvalidLabStateException("Lab instance is not running");
        }

        return eveNgService.getRawLabNodes(instance.getEveInstancePath());
    }

    private LabInstance createNewInstance(LabTemplate template, UUID userId) {
        log.info("Cloning new instance for user {} from template {}", userId, template.getName());
        
        // Path logic: /instances/{userId}/{templateName}.unl
        String sanitizedName = template.getName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        String instancePath = String.format("/instances/%s/%s.unl", userId, sanitizedName);

        // Physical copy in EVE-NG
        eveNgService.copyLab(template.getEveTemplatePath(), instancePath);

        LabInstance instance = LabInstance.builder()
                .templateId(template.getId())
                .userId(userId)
                .eveInstancePath(instancePath)
                .status(InstanceStatus.STOPPED)
                .build();

        return instanceRepository.save(instance);
    }

    /**
     * Map Template + Instance to LabResponse for Frontend.
     */
    private LabResponse toResponse(LabTemplate template, LabInstance instance) {
        // We use the existing LabResponse record structure
        // Map InstanceStatus to LabStatus for compatibility if needed, or update DTO
        return new LabResponse(
                instance != null ? instance.getId() : template.getId(), // Return instance ID if it exists, else template ID as fallback
                template.getName(),
                template.getDescription(),
                instance != null ? mapStatus(instance.getStatus()) : com.mnco.domain.entities.LabStatus.STOPPED,
                instance != null ? instance.getUserId() : null,
                template.getId(),
                instance != null ? instance.getEveInstancePath() : template.getEveTemplatePath(),
                template.getCpuAllocated(),
                template.getRamAllocated(),
                template.getStorageAllocated(),
                instance != null ? instance.getStartedAt() : null,
                instance != null ? instance.getStoppedAt() : null,
                instance != null ? instance.getCreatedAt() : template.getCreatedAt()
        );
    }

    private com.mnco.domain.entities.LabStatus mapStatus(InstanceStatus status) {
        return switch (status) {
            case RUNNING -> com.mnco.domain.entities.LabStatus.RUNNING;
            case ERROR -> com.mnco.domain.entities.LabStatus.ERROR;
            default -> com.mnco.domain.entities.LabStatus.STOPPED;
        };
    }
}
