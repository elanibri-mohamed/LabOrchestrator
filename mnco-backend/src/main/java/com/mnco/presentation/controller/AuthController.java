package com.mnco.presentation.controller;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.usecases.AuthUseCase;
import com.mnco.application.dto.response.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - email={}", request.email());
        AuthResponse response = authUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login - email=");
        AuthResponse response = authUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        log.info("POST /auth/refresh");
        AuthResponse response = authUseCase.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token) {
        log.info("POST /auth/logout");
        authUseCase.logout(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @RequestHeader("Authorization") String token) {
        log.info("GET /auth/me");
        UserDto user = authUseCase.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }
}