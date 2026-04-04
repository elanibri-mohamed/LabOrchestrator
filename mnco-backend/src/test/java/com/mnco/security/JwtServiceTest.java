package com.mnco.security;

import com.mnco.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtService — verifies token generation, validation, and claim extraction.
 * No Spring context needed.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-signing";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
        jwtService.init();
    }

    @Test
    @DisplayName("generateToken should produce a non-blank JWT string")
    void shouldGenerateNonBlankToken() {
        String token = jwtService.generateToken("anas", "STUDENT");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("extractUsername should return the subject set at generation")
    void shouldExtractUsername() {
        String token = jwtService.generateToken("anas", "STUDENT");
        assertThat(jwtService.extractUsername(token)).isEqualTo("anas");
    }

    @Test
    @DisplayName("extractRole should return the role claim set at generation")
    void shouldExtractRole() {
        String token = jwtService.generateToken("anas", "ADMIN");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("isTokenValid should return true for a freshly generated token")
    void shouldBeValidForFreshToken() {
        String token = jwtService.generateToken("anas", "STUDENT");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid should return false for a tampered token")
    void shouldBeInvalidForTamperedToken() {
        String token = jwtService.generateToken("anas", "STUDENT");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false for a completely malformed string")
    void shouldBeInvalidForGarbage() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("init should throw when secret is shorter than 32 bytes")
    void shouldThrowOnShortSecret() {
        JwtService shortKeyService = new JwtService();
        ReflectionTestUtils.setField(shortKeyService, "secret", "tooshort");
        ReflectionTestUtils.setField(shortKeyService, "expirationMs", 3600000L);

        assertThatThrownBy(shortKeyService::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
