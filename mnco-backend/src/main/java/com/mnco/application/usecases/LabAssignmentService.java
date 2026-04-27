package com.mnco.application.usecases;

import com.mnco.domain.entities.LabAssignment;
import com.mnco.domain.entities.User;
import com.mnco.domain.entities.UserRole;
import com.mnco.domain.repository.LabAssignmentRepository;
import com.mnco.domain.repository.LabTemplateRepository;
import com.mnco.domain.repository.UserRepository;
import com.mnco.exception.custom.ResourceNotFoundException;
import com.mnco.exception.custom.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabAssignmentService implements LabAssignmentUseCase {

    private final LabAssignmentRepository assignmentRepository;
    private final LabTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void assignToTeacher(UUID templateId, UUID teacherId, UUID adminId) {
        log.info("Admin {} assigning template {} to teacher {}", adminId, templateId, teacherId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigning admin not found"));

        if (!admin.isAdmin()) {
            throw new UnauthorizedException("Only admins can assign labs to teachers");
        }
        
        validateUserRole(teacherId, UserRole.TEACHER);
        
        if (assignmentRepository.existsByTemplateIdAndUserId(templateId, teacherId)) {
            log.info("Assignment already exists for teacher {}", teacherId);
            return;
        }

        LabAssignment assignment = LabAssignment.builder()
                .templateId(templateId)
                .userId(teacherId)
                .assignedBy(adminId)
                .build();
        
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void assignToStudent(UUID templateId, UUID studentId, UUID teacherId) {
        log.info("Teacher {} assigning template {} to student {}", teacherId, templateId, studentId);
        
        User assigner = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner not found"));

        if (!assigner.isAdmin() && !assigner.isTeacher()) {
            throw new UnauthorizedException("Only teachers or admins can assign labs to students");
        }

        if (assigner.isTeacher() && !assignmentRepository.existsByTemplateIdAndUserId(templateId, teacherId)) {
            throw new UnauthorizedException("Teachers can only assign labs that are assigned to them");
        }
        
        validateUserRole(studentId, UserRole.STUDENT);

        if (assignmentRepository.existsByTemplateIdAndUserId(templateId, studentId)) {
            log.info("Assignment already exists for student {}", studentId);
            return;
        }

        LabAssignment assignment = LabAssignment.builder()
                .templateId(templateId)
                .userId(studentId)
                .assignedBy(teacherId)
                .build();
        
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void revokeAssignment(UUID templateId, UUID userId, UUID revokerId, boolean isAdmin) {
        log.info("Revoking assignment for user {} on template {} by {}", userId, templateId, revokerId);
        
        LabAssignment assignment = assignmentRepository.findByTemplateIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (!isAdmin && !assignment.getAssignedBy().equals(revokerId)) {
            throw new UnauthorizedException("You can only revoke assignments you created");
        }

        assignmentRepository.deleteById(assignment.getId());
    }

    @Override
    public boolean hasAssignment(UUID templateId, UUID userId) {
        return assignmentRepository.existsByTemplateIdAndUserId(templateId, userId);
    }

    private void validateUserRole(UUID userId, UserRole expectedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (!user.getRole().equals(expectedRole)) {
            throw new UnauthorizedException("User is not a " + expectedRole);
        }
    }
}
