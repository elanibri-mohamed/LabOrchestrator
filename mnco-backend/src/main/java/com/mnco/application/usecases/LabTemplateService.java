package com.mnco.application.usecases;

import com.mnco.application.dto.request.CreateLabTemplateRequest;
import com.mnco.application.dto.response.LabTemplateResponse;
import com.mnco.application.mapper.LabTemplateMapper;
import com.mnco.domain.entities.LabTemplate;
import com.mnco.domain.repository.LabTemplateRepository;
import com.mnco.exception.custom.DuplicateResourceException;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.exception.custom.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link LabTemplateUseCase}.
 * Instructors and Admins can create templates; Students can only read public ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabTemplateService implements LabTemplateUseCase {

    private final LabTemplateRepository templateRepository;
    private final LabTemplateMapper mapper;

    @Override
    @Transactional
    public LabTemplateResponse createTemplate(CreateLabTemplateRequest request, UUID authorId) {
        log.info("Creating template '{}' by author={}", request.name(), authorId);

        if (templateRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Template name already exists: " + request.name());
        }

        LabTemplate template = new LabTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
        template.setTopologyYaml(request.topologyYaml());
        template.setVersion(request.version() != null ? request.version() : "1.0");
        template.setAuthorId(authorId);
        template.setPublic(request.isPublic());

        LabTemplate saved = templateRepository.save(template);
        log.info("Template created: id={}, name='{}'", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabTemplateResponse> getPublicTemplates() {
        return templateRepository.findAllPublic().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabTemplateResponse> getMyTemplates(UUID authorId) {
        return templateRepository.findByAuthorId(authorId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabTemplateResponse getTemplateById(UUID id) {
        return templateRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id, UUID requesterId, boolean isAdmin) {
        LabTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));

        if (!isAdmin && !template.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedException("You can only delete your own templates");
        }

        templateRepository.deleteById(id);
        log.info("Template deleted: id={}", id);
    }
}
