package com.mnco.domain.entities;

/**
 * Represents the full lifecycle state machine for a virtual lab.
 * Transitions:
 *   PENDING -> CREATING -> RUNNING -> STOPPING -> STOPPED -> DELETING -> DELETED
 *   Any state -> ERROR (on failure)
 */
public enum LabStatus {
    PENDING,
    CREATING,
    RUNNING,
    STOPPING,
    STOPPED,
    DELETING,
    ERROR,
    DELETED
}
