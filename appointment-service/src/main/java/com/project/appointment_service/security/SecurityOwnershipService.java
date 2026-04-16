package com.project.appointment_service.security;

import com.project.appointment_service.repository.AppointmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service("securityService")
public class SecurityOwnershipService {

    private final AppointmentRepository appointmentRepository;

    public SecurityOwnershipService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public boolean isAppointmentOwner(Authentication authentication, UUID appointmentId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        
        try {
            UUID tokenUserId = UUID.fromString(authentication.getName()); 
            return appointmentRepository.findById(appointmentId)
                .map(appointment -> appointment.getPatientId().equals(tokenUserId) || 
                                    appointment.getDoctorId().equals(tokenUserId))
                .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
