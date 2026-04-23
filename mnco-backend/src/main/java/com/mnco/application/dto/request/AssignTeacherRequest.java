package com.mnco.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to assign a lab template to a teacher.
 * Used by ADMINs.
 */
public record AssignTeacherRequest(
        @NotNull(message = "teacherId is required")
        UUID teacherId
) {}
