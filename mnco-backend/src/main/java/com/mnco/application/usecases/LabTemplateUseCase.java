package com.mnco.application.usecases;

import com.mnco.application.dto.request.CreateLabTemplateRequest;
import com.mnco.application.dto.response.LabTemplateResponse;

import java.util.List;
import java.util.UUID;

public interface LabTemplateUseCase {

    LabTemplateResponse createTemplate(CreateLabTemplateRequest request, UUID authorId);

    List<LabTemplateResponse> getPublicTemplates();

    List<LabTemplateResponse> getMyTemplates(UUID authorId);

    LabTemplateResponse getTemplateById(UUID id);

    void deleteTemplate(UUID id, UUID requesterId, boolean isAdmin);
}
