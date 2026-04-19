package com.project.patient_service.event;

import com.project.patient_service.model.Patient;

/**
 * Event published when a patient is updated.
 */
public record PatientUpdatedEvent(Patient patient) {
}
