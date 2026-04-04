package com.mnco.application.usecases;

import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import com.mnco.application.dto.response.AuthResponse;
import com.mnco.application.mapper.UserMapper;
import com.mnco.domain.entities.User;
import com.mnco.domain.entities.UserRole;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.DuplicateResourceException;
import com.mnco.exception.custom.InvalidCredentialsException;
import com.mnco.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — mocks all dependencies.
 * Fast, no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .username("anas")
                .email("anas@mnco.dev")
                .password("$2a$12$hashedpassword")
                .role(UserRole.STUDENT)
                .enabled(true)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register new user and return JWT")
        void shouldRegisterSuccessfully() {
            var request = new RegisterRequest("anas", "anas@mnco.dev", "Password1!");

            when(userRepository.existsByUsername("anas")).thenReturn(false);
            when(userRepository.existsByEmail("anas@mnco.dev")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("$2a$12$hashed");
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(jwtService.generateToken(anyString(), anyString())).thenReturn("jwt.token.here");
            when(jwtService.getExpirationMs()).thenReturn(86400000L);

            AuthResponse result = authService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo("jwt.token.here");
            assertThat(result.username()).isEqualTo("anas");
            assertThat(result.role()).isEqualTo(UserRole.STUDENT);

            verify(userRepository).save(argThat(u ->
                    u.getUsername().equals("anas") &&
                    u.getRole() == UserRole.STUDENT &&
                    u.isEnabled()
            ));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username taken")
        void shouldThrowWhenUsernameTaken() {
            var request = new RegisterRequest("anas", "anas@mnco.dev", "Password1!");
            when(userRepository.existsByUsername("anas")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email taken")
        void shouldThrowWhenEmailTaken() {
            var request = new RegisterRequest("anas", "anas@mnco.dev", "Password1!");
            when(userRepository.existsByUsername("anas")).thenReturn(false);
            when(userRepository.existsByEmail("anas@mnco.dev")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already registered");
        }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should authenticate with correct credentials and return JWT")
        void shouldLoginSuccessfully() {
            var request = new LoginRequest("anas", "Password1!");

            when(userRepository.findByUsername("anas")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("Password1!", sampleUser.getPassword())).thenReturn(true);
            when(jwtService.generateToken("anas", "STUDENT")).thenReturn("jwt.token.here");
            when(jwtService.getExpirationMs()).thenReturn(86400000L);

            AuthResponse result = authService.login(request);

            assertThat(result.accessToken()).isEqualTo("jwt.token.here");
            assertThat(result.username()).isEqualTo("anas");
        }

        @Test
        @DisplayName("should try email lookup when username not found")
        void shouldFallbackToEmailLookup() {
            var request = new LoginRequest("anas@mnco.dev", "Password1!");

            when(userRepository.findByUsername("anas@mnco.dev")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("anas@mnco.dev")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("Password1!", sampleUser.getPassword())).thenReturn(true);
            when(jwtService.generateToken(anyString(), anyString())).thenReturn("token");
            when(jwtService.getExpirationMs()).thenReturn(86400000L);

            AuthResponse result = authService.login(request);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException on wrong password")
        void shouldThrowOnWrongPassword() {
            var request = new LoginRequest("anas", "WrongPass!");

            when(userRepository.findByUsername("anas")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("WrongPass!", sampleUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException when user not found")
        void shouldThrowWhenUserNotFound() {
            var request = new LoginRequest("unknown", "Pass!");

            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw when account is disabled")
        void shouldThrowWhenAccountDisabled() {
            sampleUser.setEnabled(false);
            var request = new LoginRequest("anas", "Password1!");

            when(userRepository.findByUsername("anas")).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("disabled");
        }
    }
}
