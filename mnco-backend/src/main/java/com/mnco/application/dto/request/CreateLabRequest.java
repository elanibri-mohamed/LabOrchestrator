package com.mnco.application.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * Payload for creating a new virtual lab.
 */
public record CreateLabRequest(

        @NotBlank(message = "Lab name is required")
        @Size(min = 3, max = 100, message = "Lab name must be between 3 and 100 characters")
        String name,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        UUID templateId,

        @Min(value = 1, message = "CPU must be at least 1 core")
        @Max(value = 32, message = "CPU cannot exceed 32 cores")
        int cpu,

        @Min(value = 1, message = "RAM must be at least 1 GB")
        @Max(value = 128, message = "RAM cannot exceed 128 GB")
        int ram,

        @Min(value = 10, message = "Storage must be at least 10 GB")
        @Max(value = 500, message = "Storage cannot exceed 500 GB")
        int storage
) {}
