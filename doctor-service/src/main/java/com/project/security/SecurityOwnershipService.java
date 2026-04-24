package com.project.security;

import com.project.model.LeaveStatus;
import com.project.repository.LeaveAbsenceRepository;
import com.project.repository.DoctorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service("securityService")
public class SecurityOwnershipService {

    private final DoctorRepository doctorRepository;
    private final LeaveAbsenceRepository leaveAbsenceRepository;

    public SecurityOwnershipService(DoctorRepository doctorRepository, LeaveAbsenceRepository leaveAbsenceRepository) {
        this.doctorRepository = doctorRepository;
        this.leaveAbsenceRepository = leaveAbsenceRepository;
    }

    public boolean isDoctorOwner(Authentication authentication, UUID doctorId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        try {
            UUID tokenUserId = UUID.fromString(authentication.getName());
            return doctorRepository.findById(doctorId)
                    .map(doctor -> tokenUserId.equals(doctor.getAuthUserId()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isOwnPendingLeave(Authentication authentication, UUID doctorId, UUID leaveId) {
        if (!isDoctorOwner(authentication, doctorId)) {
            return false;
        }
        return leaveAbsenceRepository.findById(leaveId)
                .map(leave -> leave.getDoctorId().equals(doctorId) && leave.getStatus() == LeaveStatus.PENDING)
                .orElse(false);
    }
}
