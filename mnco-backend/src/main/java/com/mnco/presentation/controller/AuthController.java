package com.mnco.presentation.controller;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.ApiResponse;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.dto.response.UserResponse;
import com.mnco.application.usecases.AuthUseCase;
import com.mnco.application.usecases.AuditLogService;
import com.mnco.application.usecases.RefreshTokenService;
import com.mnco.domain.entities.AuditLog;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.InvalidCredentialsException;
import com.mnco.exception.custom.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * Auth controller — now includes:
 *   POST /auth/register  — register + issue access + refresh token
 *   POST /auth/login     — login + issue access + refresh token
 *   POST /auth/refresh   — rotate refresh token (FR-AA-04)
 *   POST /auth/logout    — revoke all refresh tokens (FR-AA-06)
 *   GET  /auth/me        — current user profile
 *
 * Refresh token is delivered via HttpOnly cookie (FR-AA-04 recommendation).
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authUseCase.register(request);

        // Issue refresh token and set HttpOnly cookie
        String refreshToken = refreshTokenService.issueRefreshToken(auth.userId());
        setRefreshCookie(response, refreshToken);

        // Return access token in body, refresh token in cookie
        AuthResponse withRefresh = new AuthResponse(auth.accessToken(), auth.tokenType(),
                auth.expiresIn(), auth.userId(), auth.username(), auth.email(), auth.role(), null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", withRefresh));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authUseCase.login(request);

        String refreshToken = refreshTokenService.issueRefreshToken(auth.userId());
        setRefreshCookie(response, refreshToken);

        AuthResponse withoutRefreshInBody = new AuthResponse(auth.accessToken(), auth.tokenType(),
                auth.expiresIn(), auth.userId(), auth.username(), auth.email(), auth.role(), null);

        return ResponseEntity.ok(ApiResponse.success("Login successful", withoutRefreshInBody));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            throw new InvalidCredentialsException("Refresh token cookie missing");
        }

        AuthResponse rotated = refreshTokenService.rotate(rawToken);

        // Set the new refresh token cookie
        setRefreshCookie(response, rotated.refreshToken());

        // Return new access token in body only
        AuthResponse responseBody = new AuthResponse(rotated.accessToken(), rotated.tokenType(),
                rotated.expiresIn(), rotated.userId(), rotated.username(),
                rotated.email(), rotated.role(), null);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed", responseBody));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {

        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.revokeAllForUser(user.getId());
        auditLogService.logLogout(user.getId(), user.getUsername(),
                request.getRemoteAddr());

        // Clear the cookie
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse profile = authUseCase.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);   // HTTPS only in prod
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
