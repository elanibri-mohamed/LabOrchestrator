package com.mnco.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mnco.domain.entities.UserRole;
import java.util.UUID;

/**
 * Returned after successful authentication, registration, or token refresh.
 * refreshToken is only populated on login/register/refresh — never on /me calls.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String email,
        UserRole role,
        String refreshToken   // populated on login, register, and refresh
) {
    /** Convenience factory without refresh token (e.g. for profile endpoint) */
    public static AuthResponse of(String token, long expiresIn, UUID userId,
                                   String username, String email, UserRole role) {
        return new AuthResponse(token, "Bearer", expiresIn, userId, username, email, role, null);
    }
}
