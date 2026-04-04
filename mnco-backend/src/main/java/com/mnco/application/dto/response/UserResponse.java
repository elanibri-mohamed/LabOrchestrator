package com.mnco.application.dto.response;

import com.mnco.domain.entities.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe public projection of a User — never exposes the hashed password.
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt
) {}
