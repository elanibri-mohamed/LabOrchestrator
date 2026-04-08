package com.mnco.application.usecases;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.dto.response.UserResponse;
import com.mnco.application.mapper.UserMapper;
import com.mnco.domain.entities.AuditLog.EventType;
import com.mnco.domain.entities.AuditLog.Result;
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
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Token refresh attempt");
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found: " + username));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Account is disabled");
        }

        String newToken = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.of(newToken, jwtService.getExpirationMs(),
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    @Transactional
    public void logout(String token) {
        log.info("Logout attempt");
        // Token invalidation logic would go here
        // For now, this is a no-op as tokens are stateless
        // In a production system, you might add the token to a blacklist
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String token) {
        log.info("Get current user attempt");
        String username = jwtService.extractUsername(token);
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
