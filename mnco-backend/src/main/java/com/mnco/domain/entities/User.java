package com.mnco.domain.entities;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain entity representing a platform user.
 * This is a pure domain object — no JPA annotations here.
 * Persistence concerns are handled in the infrastructure layer.
 */
public class User {

    private UUID id;
    private String username;
    private String email;
    private String password;
    private UserRole role;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.role = builder.role;
        this.enabled = builder.enabled;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    // ── Domain behaviour ──────────────────────────────────────────────────────

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    public boolean isInstructor() {
        return UserRole.INSTRUCTOR.equals(this.role);
    }

    public boolean canManageTemplates() {
        return UserRole.ADMIN.equals(this.role) || UserRole.INSTRUCTOR.equals(this.role);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private String username;
        private String email;
        private String password;
        private UserRole role = UserRole.STUDENT;
        private boolean enabled = true;
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public User build() { return new User(this); }
    }
}
