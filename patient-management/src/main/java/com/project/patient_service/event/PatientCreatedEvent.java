package com.project.patient_service.event;

import com.project.patient_service.model.Patient;

/**
 * Event published when a new patient is created.
 */
public record PatientCreatedEvent(Patient patient) {
}
