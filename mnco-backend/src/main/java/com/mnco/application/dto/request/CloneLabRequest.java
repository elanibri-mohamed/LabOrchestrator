package com.mnco.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for cloning an existing lab (FR-LM-06).
 * Source lab must be in STOPPED status.
 */
public record CloneLabRequest(

        @NotBlank(message = "Clone name is required")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @Size(max = 500)
        String description
) {}
