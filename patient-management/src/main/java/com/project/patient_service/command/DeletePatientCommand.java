package com.project.patient_service.command;

import java.util.UUID;

/**
 * Command to delete a patient.
 */
public record DeletePatientCommand(UUID id) {
}
