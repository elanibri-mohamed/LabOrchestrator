package com.mnco.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mnco.BaseIntegrationTest;
import com.mnco.application.dto.request.LoginRequest;
import com.mnco.application.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Full Spring context integration tests for the /auth endpoints.
 * Uses H2 in-memory DB and EVE-NG simulation mode.
 */
@DisplayName("AuthController Integration Tests")
class AuthControllerIT extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register — 201 for valid payload")
    void shouldReturn201OnValidRegistration() throws Exception {
        var request = new RegisterRequest("newuser_it", "newuser@test.com", "SecurePass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("newuser_it"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register — 409 for duplicate username")
    void shouldReturn409OnDuplicateUsername() throws Exception {
        var first  = new RegisterRequest("dupuser", "first@test.com",  "SecurePass1!");
        var second = new RegisterRequest("dupuser", "second@test.com", "SecurePass1!");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Username already taken")));
    }

    @Test
    @DisplayName("POST /auth/register — 400 for missing required fields")
    void shouldReturn400ForInvalidPayload() throws Exception {
        var badRequest = """
                { "username": "a", "email": "not-an-email", "password": "short" }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("POST /auth/register — 400 for blank username")
    void shouldReturn400ForBlankUsername() throws Exception {
        var badRequest = """
                { "username": "", "email": "valid@test.com", "password": "ValidPass1!" }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login — 200 with JWT for correct credentials")
    void shouldReturn200OnSuccessfulLogin() throws Exception {
        // First register the user
        var register = new RegisterRequest("loginuser", "login@test.com", "SecurePass1!");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        // Then login
        var loginReq = new LoginRequest("loginuser", "SecurePass1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("loginuser"));
    }

    @Test
    @DisplayName("POST /auth/login — 401 for wrong password")
    void shouldReturn401ForWrongPassword() throws Exception {
        var register = new RegisterRequest("passcheck", "passcheck@test.com", "CorrectPass1!");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));

        var loginReq = new LoginRequest("passcheck", "WrongPassword!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").doesNotExist()); // ErrorResponse, not ApiResponse
    }

    @Test
    @DisplayName("POST /auth/login — 401 for unknown user")
    void shouldReturn401ForUnknownUser() throws Exception {
        var loginReq = new LoginRequest("nobody_exists", "SomePass1!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/me — 200 with profile for authenticated user")
    void shouldReturnProfileForAuthenticatedUser() throws Exception {
        // Register and grab token
        var register = new RegisterRequest("meuser", "meuser@test.com", "SecurePass1!");
        var registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        var responseBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = responseBody.path("data").path("accessToken").asText();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("meuser"))
                .andExpect(jsonPath("$.data.email").value("meuser@test.com"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    @DisplayName("GET /auth/me — 403 without authorization header")
    void shouldReturn403WithoutToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isForbidden());
    }
}
