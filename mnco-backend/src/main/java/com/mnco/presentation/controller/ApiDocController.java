package com.mnco.presentation.controller;

import com.mnco.application.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint exposing the full API contract of the MNCO platform.
 *
 * GET /api-docs/endpoints — returns all available endpoints with method, path,
 *                           auth requirement, roles, and description.
 *
 * Public — no authentication required.
 * Changing EVE-NG or the frontend never affects this contract.
 */
@RestController
@RequestMapping("/api-docs")
@RequiredArgsConstructor
public class ApiDocController {

    @GetMapping("/endpoints")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEndpoints() {

        var endpoints = List.of(

            // ── Auth ─────────────────────────────────────────────────────────
            endpoint("POST", "/auth/register",
                "Register a new user account", false, null,
                body("username", "string — 3 to 50 chars"),
                body("email",    "string — valid email"),
                body("password", "string — 8 to 100 chars"),
                returns("accessToken, tokenType, expiresIn, userId, username, email, role")),

            endpoint("POST", "/auth/login",
                "Authenticate and receive a JWT access token", false, null,
                body("usernameOrEmail", "string"),
                body("password",        "string"),
                returns("accessToken, tokenType, expiresIn, userId, username, email, role")),

            endpoint("POST", "/auth/refresh",
                "Rotate refresh token — reads HttpOnly cookie, sets new cookie", false, null,
                returns("accessToken, tokenType, expiresIn, userId, username, email, role")),

            endpoint("POST", "/auth/logout",
                "Revoke all refresh tokens and clear cookie", true, null,
                returns("success message")),

            endpoint("GET", "/auth/me",
                "Get current authenticated user profile", true, null,
                returns("id, username, email, role, enabled, createdAt")),

            // ── Labs ─────────────────────────────────────────────────────────
            endpoint("GET", "/labs",
                "List labs — own labs for users, all labs for ADMIN", true, null,
                returns("list of labs with id, name, status, cpu, ram, storage, dates")),

            endpoint("POST", "/labs",
                "Create a new lab (quota enforced)", true, null,
                body("name",        "string"),
                body("description", "string — optional"),
                body("templateId",  "UUID — optional"),
                body("cpu",         "int — 1 to 32"),
                body("ram",         "int — 1 to 128"),
                body("storage",     "int — 10 to 500"),
                returns("created lab object")),

            endpoint("GET", "/labs/{id}",
                "Get a single lab by ID (owner or ADMIN)", true, null,
                returns("lab detail object")),

            endpoint("POST", "/labs/{id}/start",
                "Start a STOPPED lab via EVE-NG", true, null,
                returns("updated lab with status RUNNING")),

            endpoint("POST", "/labs/{id}/stop",
                "Stop a RUNNING lab via EVE-NG", true, null,
                returns("updated lab with status STOPPED")),

            endpoint("POST", "/labs/{id}/clone",
                "Deep-copy a STOPPED lab into a new independent lab", true, null,
                body("name",        "string — name for the cloned lab"),
                body("description", "string — optional"),
                returns("new cloned lab object")),

            endpoint("DELETE", "/labs/{id}",
                "Delete a STOPPED or ERROR lab and release quota", true, null,
                returns("success message")),

            endpoint("GET", "/labs/{id}/nodes/{nodeId}/console",
                "Get console connection info for a node (lab must be RUNNING)", true, null,
                returns("protocol, host, port, webSocketUrl, nodeId, nodeName, nodeStatus")),

            endpoint("GET", "/labs/admin/all",
                "List all labs platform-wide", true, new String[]{"ADMIN"},
                returns("list of all labs")),

            // ── Templates ────────────────────────────────────────────────────
            endpoint("GET", "/templates",
                "List all public lab templates", false, null,
                returns("list of templates")),

            endpoint("GET", "/templates/mine",
                "List templates authored by the current user", true, null,
                returns("list of templates")),

            endpoint("GET", "/templates/{id}",
                "Get a single template by ID", false, null,
                returns("template detail")),

            endpoint("POST", "/templates",
                "Create a new lab template", true, new String[]{"INSTRUCTOR", "ADMIN"},
                body("name",         "string"),
                body("description",  "string"),
                body("topologyYaml", "string — EVE-NG topology definition"),
                body("version",      "string — e.g. 1.0.0"),
                body("isPublic",     "boolean"),
                returns("created template object")),

            endpoint("DELETE", "/templates/{id}",
                "Delete a template (author or ADMIN only)", true, new String[]{"INSTRUCTOR", "ADMIN"},
                returns("success message")),

            // ── Quota ────────────────────────────────────────────────────────
            endpoint("GET", "/quota/me",
                "Get current user's resource quota usage and limits", true, null,
                returns("maxLabs, usedLabs, remainingLabs, maxCpu, usedCpu, remainingCpu, " +
                        "maxRamGb, usedRamGb, remainingRamGb, maxStorageGb, usedStorageGb, remainingStorageGb")),

            // ── Admin ────────────────────────────────────────────────────────
            endpoint("GET", "/admin/users",
                "List all platform users", true, new String[]{"ADMIN"},
                returns("list of users")),

            endpoint("GET", "/admin/users/{id}",
                "Get a user by ID", true, new String[]{"ADMIN"},
                returns("user object")),

            endpoint("PATCH", "/admin/users/{id}/role",
                "Change a user's role — query param: role=ADMIN|INSTRUCTOR|STUDENT|RESEARCHER",
                true, new String[]{"ADMIN"},
                returns("updated user object")),

            endpoint("DELETE", "/admin/users/{id}",
                "Disable a user account", true, new String[]{"ADMIN"},
                returns("success message")),

            endpoint("GET", "/admin/users/{id}/quota",
                "View a user's quota", true, new String[]{"ADMIN"},
                returns("quota object")),

            endpoint("PUT", "/admin/users/{id}/quota",
                "Override a user's quota limits", true, new String[]{"ADMIN"},
                body("maxLabs",      "int — 1 to 50"),
                body("maxCpu",       "int — 1 to 128"),
                body("maxRamGb",     "int — 1 to 512"),
                body("maxStorageGb", "int — 10 to 2000"),
                returns("updated quota object")),

            // ── Audit ────────────────────────────────────────────────────────
            endpoint("GET", "/audit/recent?limit=100",
                "Get most recent N audit log entries (max 500)", true, new String[]{"ADMIN"},
                returns("list of audit log entries")),

            endpoint("GET", "/audit/user/{userId}",
                "Get all audit entries for a specific user", true, new String[]{"ADMIN"},
                returns("list of audit log entries")),

            endpoint("GET", "/audit/lab/{labId}",
                "Get all audit entries for a specific lab", true, new String[]{"ADMIN"},
                returns("list of audit log entries"))
        );

        Map<String, Object> contract = Map.of(
            "platform",        "MNCO — Multi-Tenant Network and Cybersecurity Lab Orchestrator",
            "version",         "1.0.0",
            "baseUrl",         "/",
            "authentication",  "Bearer JWT — Authorization: Bearer <token>",
            "responseFormat",  "{ success, message, data, timestamp }",
            "labStateMachine", List.of(
                "PENDING → CREATING → STOPPED → RUNNING → STOPPING → STOPPED",
                "CREATING → ERROR",
                "RUNNING  → ERROR",
                "STOPPED  → DELETING → DELETED"
            ),
            "endpoints", endpoints
        );

        return ResponseEntity.ok(ApiResponse.success(contract));
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private Map<String, Object> endpoint(String method, String path, String description,
                                          boolean requiresAuth, String[] roles,
                                          Map<String, String>... extras) {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("method",       method);
        map.put("path",         path);
        map.put("description",  description);
        map.put("requiresAuth", requiresAuth);
        map.put("roles",        roles != null ? List.of(roles) : "any authenticated user");

        for (var extra : extras) {
            String type = extra.get("__type");
            if ("body".equals(type)) {
                map.computeIfAbsent("requestBody",
                        k -> new java.util.LinkedHashMap<String, String>());
                ((Map<String, String>) map.get("requestBody"))
                        .put(extra.get("field"), extra.get("desc"));
            } else if ("returns".equals(type)) {
                map.put("returns", extra.get("value"));
            }
        }
        return map;
    }

    private Map<String, String> body(String field, String desc) {
        return Map.of("__type", "body", "field", field, "desc", desc);
    }

    private Map<String, String> returns(String value) {
        return Map.of("__type", "returns", "value", value);
    }
}