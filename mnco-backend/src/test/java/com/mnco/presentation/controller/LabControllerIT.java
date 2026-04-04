package com.mnco.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mnco.BaseIntegrationTest;
import com.mnco.application.dto.request.CreateLabRequest;
import com.mnco.application.dto.request.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Spring context integration tests for the /labs endpoints.
 * Each test class registers its own unique user to avoid state collisions.
 */
@DisplayName("LabController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabControllerIT extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String authToken;
    private static String createdLabId;

    // ── Helper: register + get token ──────────────────────────────────────────

    private String registerAndGetToken(String username, String email) throws Exception {
        var register = new RegisterRequest(username, email, "SecurePass1!");
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    @BeforeEach
    void setUpToken() throws Exception {
        if (authToken == null) {
            authToken = registerAndGetToken("labuser_" + System.nanoTime(),
                    "labuser" + System.nanoTime() + "@test.com");
        }
    }

    // ── POST /api/labs ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /labs — 201 for valid lab creation")
    void shouldCreateLabSuccessfully() throws Exception {
        var request = new CreateLabRequest("My First Lab", "Integration test lab", null, 2, 4, 20);

        MvcResult result = mockMvc.perform(post("/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("My First Lab"))
                .andExpect(jsonPath("$.data.status").value("STOPPED"))
                .andExpect(jsonPath("$.data.cpuAllocated").value(2))
                .andExpect(jsonPath("$.data.ramAllocated").value(4))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        createdLabId = body.path("data").path("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("POST /labs — 400 for invalid payload (missing name)")
    void shouldReturn400ForMissingName() throws Exception {
        var badRequest = """
                { "cpu": 2, "ram": 4, "storage": 20 }
                """;

        mockMvc.perform(post("/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + authToken)
                        .content(badRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /labs — 403 without auth token")
    void shouldReturn403WithoutToken() throws Exception {
        var request = new CreateLabRequest("Unauthorized Lab", null, null, 1, 1, 10);
        mockMvc.perform(post("/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/labs ─────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("GET /labs — 200 with list of user labs")
    void shouldListUserLabs() throws Exception {
        mockMvc.perform(get("/labs")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    // ── GET /api/labs/{id} ────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /labs/{id} — 200 for own lab")
    void shouldGetLabById() throws Exception {
        Assumptions.assumeTrue(createdLabId != null, "Lab must be created first");

        mockMvc.perform(get("/labs/{id}", createdLabId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdLabId));
    }

    @Test
    @Order(6)
    @DisplayName("GET /labs/{id} — 404 for non-existent lab")
    void shouldReturn404ForNonExistentLab() throws Exception {
        mockMvc.perform(get("/labs/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/labs/{id}/start ─────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("POST /labs/{id}/start — 200 lab transitions to RUNNING")
    void shouldStartLab() throws Exception {
        Assumptions.assumeTrue(createdLabId != null, "Lab must be created first");

        mockMvc.perform(post("/labs/{id}/start", createdLabId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /labs/{id}/start — 422 when lab already RUNNING")
    void shouldReturn422WhenAlreadyRunning() throws Exception {
        Assumptions.assumeTrue(createdLabId != null, "Lab must be created first");

        mockMvc.perform(post("/labs/{id}/start", createdLabId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /api/labs/{id}/stop ──────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("POST /labs/{id}/stop — 200 lab transitions to STOPPED")
    void shouldStopLab() throws Exception {
        Assumptions.assumeTrue(createdLabId != null, "Lab must be created first");

        mockMvc.perform(post("/labs/{id}/stop", createdLabId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STOPPED"));
    }

    // ── DELETE /api/labs/{id} ─────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("DELETE /labs/{id} — 200 for own STOPPED lab")
    void shouldDeleteStoppedLab() throws Exception {
        Assumptions.assumeTrue(createdLabId != null, "Lab must be created first");

        mockMvc.perform(delete("/labs/{id}", createdLabId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lab deleted successfully"));
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /labs/{id} — 404 after deletion")
    void shouldReturn404AfterDeletion() throws Exception {
        Assumptions.assumeTrue(createdLabId != null, "Lab must be created first");

        // Lab is now DELETED status — should be invisible
        mockMvc.perform(get("/labs/{id}", createdLabId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
