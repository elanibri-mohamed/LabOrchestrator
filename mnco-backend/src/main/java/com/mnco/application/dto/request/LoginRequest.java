package com.mnco.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for user authentication.
 */
public record LoginRequest(

        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {}
