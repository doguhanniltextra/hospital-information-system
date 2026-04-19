package com.project.patient_service.command;

import com.project.patient_service.dto.request.UpdatePatientServiceRequestDto;
import java.util.UUID;

/**
 * Command to update an existing patient.
 */
public record UpdatePatientCommand(UUID id, UpdatePatientServiceRequestDto updateRequest) {
}
