package com.mnco.application.usecases;

import java.util.UUID;

/**
 * Port for Lab Assignment management.
 */
public interface LabAssignmentUseCase {

    void assignToTeacher(UUID templateId, UUID teacherId, UUID adminId);

    void assignToStudent(UUID templateId, UUID studentId, UUID teacherId);

    void revokeAssignment(UUID templateId, UUID userId, UUID revokerId, boolean isAdmin);
    
    boolean hasAssignment(UUID templateId, UUID userId);
}
