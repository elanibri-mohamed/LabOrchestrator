package com.mnco.application.usecases;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.dto.response.UserResponse;

/**
 * Port (interface) for authentication use cases.
 * Keeps the application layer decoupled from any specific implementation.
 */
public interface AuthUseCase {

    /**
     * Registers a new user with STUDENT role and returns an auth token.
     *
     * @param request validated registration payload
     * @return auth response containing JWT and user details
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user by username/email + password.
     *
     * @param request login credentials
     * @return auth response containing JWT and user details
     */
    AuthResponse login(LoginRequest request);

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param username the authenticated principal's username
     */
    UserResponse getProfile(String username);
}
