package com.mnco.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Service responsible for JWT generation, validation, and claim extraction.
 * Uses HMAC-SHA256 signing with a configurable secret key.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        // Ensure key material is at least 256 bits for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT service initialized with expiration={}ms", expirationMs);
    }

    /**
     * Generates a signed JWT token for the given user.
     *
     * @param username the subject of the token (username)
     * @param role     the user's role, embedded as a claim
     * @return signed JWT string
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claims(Map.of("role", role))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a refresh token for the given user.
     *
     * @param username the subject of the token (username)
     * @return signed refresh token
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expirationMs * 10)); // Longer expiry for refresh tokens

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return the username embedded in the token
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from a JWT token.
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Validates the token's signature and expiry. Does NOT check against the database.
     *
     * @param token the raw JWT string
     * @return true if the token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Validates a refresh token and returns the username if valid.
     *
     * @param token the refresh token
     * @return the username embedded in the token
     */
    public String validateRefreshToken(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (JwtException e) {
            log.error("Invalid refresh token", e);
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    /**
     * Revokes a token (e.g., by adding it to a blacklist).
     * This is a placeholder for actual revocation logic.
     *
     * @param token the token to revoke
     */
    public void revokeToken(String token) {
        log.info("Token revoked: {}", token);
        // Implement token revocation logic (e.g., add to blacklist)
    }

    /**
     * Returns the configured token expiration in milliseconds.
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private Claims extractClaims(String token) {
        if (token == null) {
            throw new IllegalArgumentException("JWT token is null");
        }
        String sanitizedToken = token.trim();
        if (sanitizedToken.toLowerCase().startsWith("bearer ")) {
            sanitizedToken = sanitizedToken.substring(7).trim();
        }
        if (sanitizedToken.contains(" ")) {
            throw new io.jsonwebtoken.MalformedJwtException("Compact JWT strings may not contain whitespace.");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(sanitizedToken)
                .getPayload();
    }
}
