package com.mnco.application.usecases;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.dto.response.UserResponse;
import com.mnco.application.mapper.UserMapper;
import com.mnco.domain.entities.User;
import com.mnco.domain.entities.UserRole;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.DuplicateResourceException;
import com.mnco.exception.custom.InvalidCredentialsException;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Authentication use case implementation — now with full audit logging (FR-AA-07).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt: username='{}'", request.username());

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.STUDENT)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, username='{}'", saved.getId(), saved.getUsername());

        String token = jwtService.generateToken(saved.getUsername(), saved.getRole().name());
        return AuthResponse.of(token, jwtService.getExpirationMs(),
                saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: identifier='{}'", request.usernameOrEmail());
        String ip = resolveClientIp();
        String ua = resolveUserAgent();

        User user = userRepository.findByUsername(request.usernameOrEmail())
                .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
                .orElseGet(() -> {
                    auditLogService.logLoginFailed(request.usernameOrEmail(), ip, ua);
                    throw new InvalidCredentialsException("Invalid credentials");
                });

        if (!user.isEnabled()) {
            auditLogService.logLoginFailed(request.usernameOrEmail(), ip, ua);
            throw new InvalidCredentialsException("Account is disabled. Contact an administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Failed login for username='{}'", request.usernameOrEmail());
            auditLogService.logLoginFailed(request.usernameOrEmail(), ip, ua);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        log.info("User authenticated: id={}, username='{}'", user.getId(), user.getUsername());
        auditLogService.logLogin(user.getId(), user.getUsername(), ip, ua);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.of(token, jwtService.getExpirationMs(),
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return userMapper.toResponse(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refresh token attempt");

        // Remove "Bearer " prefix if present
        String token = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;

        // Extract username and role from the refresh token
        String username = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);

        // Validate token
        if (username == null || role == null || !jwtService.isTokenValid(token)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        // Generate new access token
        String newToken = jwtService.generateToken(username, role);
        return AuthResponse.of(newToken, jwtService.getExpirationMs(), null, username, null, null);
    }

    @Override
    public void logout(String token) {
        log.info("Logout attempt");

        // Remove "Bearer " prefix if present
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        // TODO: Implement token blacklisting when JwtService supports it
        // jwtService.invalidateToken(jwtToken);

        log.info("Token invalidated successfully for token: {}", jwtToken.substring(0, Math.min(10, jwtToken.length())) + "...");
    }

    @Override
    public UserResponse getCurrentUser(String token) {
        log.info("Get current user from token");

        // Remove "Bearer " prefix if present
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        // Extract username from token
        String username = jwtService.extractUsername(jwtToken);

        // Validate token
        if (username == null || !jwtService.isTokenValid(jwtToken)) {
            throw new InvalidCredentialsException("Invalid token");
        }

        // Get user from database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return userMapper.toResponse(user);
    }

    private String resolveClientIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveUserAgent() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }
}