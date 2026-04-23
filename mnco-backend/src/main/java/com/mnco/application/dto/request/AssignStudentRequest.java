package com.mnco.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to assign a lab template to a student.
 * Used by TEACHERs.
 */
public record AssignStudentRequest(
        @NotNull(message = "studentId is required")
        UUID studentId
) {}
