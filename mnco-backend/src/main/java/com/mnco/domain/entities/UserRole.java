package com.mnco.domain.entities;

/**
 * Defines the role-based access levels in the MNCO platform.
 * Maps directly to Spring Security GrantedAuthority values.
 */
public enum UserRole {
    ADMIN,
    INSTRUCTOR,
    STUDENT,
    RESEARCHER
}
