package com.mnco.application.usecases;

import com.mnco.application.dto.response.AuthResponse;
import com.mnco.domain.entities.AuditLog;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.InvalidCredentialsException;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.mnco.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.mnco.security.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Manages refresh token lifecycle: issuance, rotation, and revocation.
 *
 * Per SRS FR-AA-04:
 *  - Tokens are 256-bit random values, stored directly (not bcrypt — they're
 *    already high-entropy random values, bcrypt would be redundant overhead).
 *  - Each use ROTATES the token: old one is revoked, new one is issued.
 *  - Replaying an old token → 401 Unauthorized.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    // ── Issue ─────────────────────────────────────────────────────────────────

    /**
     * Issues a new refresh token for the given user.
     * Revokes all existing tokens for the user first (single active session policy).
     */
    @Transactional
    public String issueRefreshToken(UUID userId) {
        // Revoke any existing refresh tokens for this user
        refreshTokenRepository.revokeAllForUser(userId);

        String rawToken = generateSecureToken();
        Instant expiresAt = Instant.now().plus(refreshExpirationMs, ChronoUnit.MILLIS);

        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .userId(userId)
                .token(rawToken)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        log.debug("Refresh token issued for userId={}", userId);
        return rawToken;
    }

    // ── Rotate ────────────────────────────────────────────────────────────────

    /**
     * Validates the provided refresh token, revokes it, issues a new access+refresh token pair.
     * Implements token rotation as required by FR-AA-04.
     */
    @Transactional
    public AuthResponse rotate(String rawToken) {
        RefreshTokenJpaEntity storedToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found — possible replay attack");
                    return new InvalidCredentialsException("Invalid or expired refresh token");
                });

        if (storedToken.isRevoked()) {
            log.warn("Revoked refresh token used — userId={}, possible token theft",
                    storedToken.getUserId());
            // Revoke ALL tokens for this user as a precaution (token theft response)
            refreshTokenRepository.revokeAllForUser(storedToken.getUserId());
            throw new InvalidCredentialsException("Refresh token has been revoked. Please log in again.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidCredentialsException("Refresh token has expired. Please log in again.");
        }

        // Revoke the used token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Load user and issue new token pair
        var user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Account is disabled");
        }

        String newAccessToken  = jwtService.generateToken(user.getUsername(), user.getRole().name());
        String newRefreshToken = issueRefreshToken(user.getId());

        auditLogService.logTokenRefresh(user.getId(), user.getUsername(), null);
        log.info("Refresh token rotated for userId={}", user.getId());

        return new AuthResponse(newAccessToken, "Bearer", jwtService.getExpirationMs(),
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                newRefreshToken);
    }

    // ── Revoke all (logout) ───────────────────────────────────────────────────

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        log.debug("All refresh tokens revoked for userId={}", userId);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 3 * * *") // 3am every day
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens();
        log.info("Purged expired refresh tokens");
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
