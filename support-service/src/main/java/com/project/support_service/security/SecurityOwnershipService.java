package com.project.support_service.security;

import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.PatientIdentity;
import com.project.support_service.repository.LabOrderRepository;
import com.project.support_service.repository.PatientIdentityRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service("securityOwnershipService")
public class SecurityOwnershipService {
    private final LabOrderRepository labOrderRepository;
    private final PatientIdentityRepository patientIdentityRepository;

    public SecurityOwnershipService(LabOrderRepository labOrderRepository,
                                   PatientIdentityRepository patientIdentityRepository) {
        this.labOrderRepository = labOrderRepository;
        this.patientIdentityRepository = patientIdentityRepository;
    }

    public boolean isLabOrderOwner(Authentication authentication, UUID orderId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String authUserId = authentication.getName();
        return labOrderRepository.findById(orderId)
                .flatMap(order -> patientIdentityRepository.findById(order.getPatientId()))
                .map(identity -> identity.getAuthUserId().equals(authUserId))
                .orElse(false);
    }

    public UUID getPatientIdForAuthUser(String authUserId) {
        return patientIdentityRepository.findByAuthUserId(authUserId)
                .map(PatientIdentity::getPatientId)
                .orElse(null);
    }
}
