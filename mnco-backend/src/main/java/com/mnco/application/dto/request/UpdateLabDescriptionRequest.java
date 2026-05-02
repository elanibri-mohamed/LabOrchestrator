package com.mnco.application.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateLabDescriptionRequest(

        @Size(max = 100000, message = "Description cannot exceed 100000 characters")
        String description
) {}