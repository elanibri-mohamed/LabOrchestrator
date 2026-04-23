package com.mnco.domain.entities;

/**
 * Instance runtime state.
 * Aligns with reference architecture instance_status enum.
 *   STOPPED — instance exists but all nodes are powered off
 *   RUNNING — at least one node is powered on (EVE-NG reports nodes RUNNING)
 *   ERROR   — EVE-NG operation failed or instance corrupted
 */
public enum InstanceStatus {
    STOPPED,
    RUNNING,
    ERROR
}
