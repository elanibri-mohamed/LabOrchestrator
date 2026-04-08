package com.mnco.application.usecases;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.dto.response.UserResponse;

public interface AuthUseCase {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getProfile(String username);

    AuthResponse refreshToken(String refreshToken);

    void logout(String token);

    UserResponse getCurrentUser(String token);
}