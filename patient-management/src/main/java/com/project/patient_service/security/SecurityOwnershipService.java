package com.project.patient_service.security;

import com.project.patient_service.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service("securityService")
public class SecurityOwnershipService {

    private final PatientRepository patientRepository;

    public SecurityOwnershipService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public boolean isPatientOwner(Authentication authentication, UUID patientId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        try {
            UUID tokenUserId = UUID.fromString(authentication.getName());
            return patientRepository.findById(patientId)
                    .map(patient -> tokenUserId.equals(patient.getAuthUserId()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
