package com.mnco.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLabTemplateRequest(

        @NotBlank(message = "Template name is required")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        String topologyYaml,

        @Size(max = 20, message = "Version string too long")
        String version,

        boolean isPublic
) {}
