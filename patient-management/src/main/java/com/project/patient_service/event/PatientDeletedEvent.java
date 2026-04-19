package com.project.patient_service.event;

import java.util.UUID;

/**
 * Event published when a patient is deleted.
 */
public record PatientDeletedEvent(UUID id) {
}
