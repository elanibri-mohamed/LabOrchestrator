package com.mnco.application.security;

import com.mnco.domain.entities.User;
import com.mnco.domain.entities.UserRole;
import com.mnco.domain.repository.EvengTemplateRepository;
import com.mnco.domain.repository.LabAssignmentRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.AccessDeniedException;
import com.mnco.exception.custom.LabUnavailableException;
import com.mnco.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Enforces the Three-Guard pattern for multi-tenant isolation (reference section 5).
 *
 * Guard order:
 *   1. ROLE — in-memory check against role enum
 *   2. TEMPLATE STATUS — template exists and is ACTIVE
 *   3. ASSIGNMENT — assignment row exists for (template, user)
 *
 * For teacher-managed student actions, additional check: student's assigned_by must be teacherId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceGuard {

    private final EvengTemplateRepository templateRepository;
    private final LabAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    // ── Guard 1: Role checks ──────────────────────────────────────────────────

    public void ensureAdmin(UUID userId, UserRole role) {
        if (!UserRole.ADMIN.equals(role)) {
            log.warn("Access denied: user={} requires ADMIN role", userId);
            throw new AccessDeniedException("Admin role required");
        }
    }

    public void ensureTeacher(UUID userId, UserRole role) {
        if (!UserRole.INSTRUCTOR.equals(role)) {
            log.warn("Access denied: user={} requires TEACHER (INSTRUCTOR) role", userId);
            throw new AccessDeniedException("Teacher role required");
        }
    }

    public void ensureStudentOrTeacher(UUID userId, UserRole role) {
        boolean allowed = UserRole.INSTRUCTOR.equals(role) ||
                UserRole.STUDENT.equals(role) ||
                UserRole.RESEARCHER.equals(role);
        if (!allowed) {
            log.warn("Access denied: user={} requires STUDENT or TEACHER role", userId);
            throw new AccessDeniedException("Student or Teacher role required");
        }
    }

    // ── Guard 2: Template exists and ACTIVE ───────────────────────────────────

    public void ensureTemplateActive(UUID templateId) {
        var template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        if (!template.isActive()) {
            log.warn("Template inactive: id={}, status={}", templateId, template.getStatus());
            throw new LabUnavailableException("Template is not available (status: " + template.getStatus() + ")");
        }
    }

    // ── Guard 3: Assignment existence ─────────────────────────────────────────

    public void ensureUserAssigned(UUID templateId, UUID userId) {
        boolean assigned = assignmentRepository.existsByTemplateIdAndUserId(templateId, userId);
        if (!assigned) {
            log.warn("Access denied: user={} has no assignment for template={}", userId, templateId);
            throw new AccessDeniedException("You are not assigned to this lab template");
        }
    }

    /**
     * For teacher actions on a student's instance: ensure that
     *   (a) teacher is assigned to the template, and
     *   (b) student is assigned to the template, and
     *   (c) student's assignment row has assigned_by = teacherId.
     */
    public void ensureTeacherManagesStudent(UUID templateId, UUID teacherId, UUID studentId) {
        // Teacher must be assigned to template
        var teacherAssign = assignmentRepository.findByTemplateIdAndUserId(templateId, teacherId);
        if (teacherAssign.isEmpty()) {
            log.warn("Teacher {} not assigned to template {}", teacherId, templateId);
            throw new AccessDeniedException("Teacher not assigned to this template");
        }
        // Student must be assigned
        var studentAssign = assignmentRepository.findByTemplateIdAndUserId(templateId, studentId);
        if (studentAssign.isEmpty()) {
            log.warn("Student {} not assigned to template {}", studentId, templateId);
            throw new AccessDeniedException("Student not assigned to this template");
        }
        // Assignment must be made by this teacher
        if (!studentAssign.get().getAssignedBy().equals(teacherId)) {
            log.warn("Teacher {} cannot manage student {}: not the assigning teacher", teacherId, studentId);
            throw new AccessDeniedException("You are not the assigning teacher for this student");
        }
    }

    // ── Composite guards for common operations ────────────────────────────────

    /**
     * Full guard for any template access (list, start, console):
     *   - User must be assigned
     *   - Template must be ACTIVE
     */
    public void guardTemplateAccess(UUID templateId, UUID userId, UserRole role) {
        ensureStudentOrTeacher(userId, role);
        ensureTemplateActive(templateId);
        ensureUserAssigned(templateId, userId);
    }

    /**
     * Guard for teacher assigning a student:
     *   - Caller must be ADMIN? Actually ADMIN assigns teachers; TEACHER assigns students.
     *   For teacher assigning student:
     *   - Caller must be TEACHER
     *   - Template ACTIVE
     *   - Target student exists and is a student-level role
     */
    public void guardAssignStudent(UUID templateId, UUID teacherId, UserRole teacherRole, UUID studentId) {
        ensureTeacher(teacherId, teacherRole);
        ensureTemplateActive(templateId);
        // Student must exist and be student-level
        var student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (!(student.isStudent())) {
            throw new AccessDeniedException("Target user is not a student");
        }
        // Teacher must be assigned to template (implied by guard above)
        ensureUserAssigned(templateId, teacherId);
    }

    /**
     * Guard for teacher stopping/starting/resetting a student's instance:
     *   Caller must be TEACHER and must be the assigning teacher for that student.
     */
    public void guardTeacherManageStudentInstance(UUID templateId, UUID teacherId, UserRole role, UUID studentId) {
        ensureTeacher(teacherId, role);
        ensureTemplateActive(templateId);
        ensureTeacherManagesStudent(templateId, teacherId, studentId);
    }
}
