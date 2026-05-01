package com.project.patient_service.security;

import com.project.patient_service.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Service for verifying ownership and authorization for patient records.
 * Provides security checks used in @PreAuthorize annotations.
 */
@Service("securityService")
public class SecurityOwnershipService {

    private final PatientRepository patientRepository;

    /**
     * Initializes the security service with the patient repository.
     * 
     * @param patientRepository Repository for accessing core patient data
     */
    public SecurityOwnershipService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Checks if the currently authenticated user is the owner of the specified patient record.
     * Used for Attribute-Based Access Control (ABAC).
     * 
     * @param authentication The current authentication context
     * @param patientId The UUID of the clinical record being accessed
     * @return True if the user's authUserId matches the record's authUserId
     */
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

