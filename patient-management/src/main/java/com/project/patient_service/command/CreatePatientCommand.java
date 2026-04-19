package com.project.patient_service.command;

import com.project.patient_service.dto.request.CreatePatientServiceRequestDto;

/**
 * Command to create a new patient.
 */
public record CreatePatientCommand(CreatePatientServiceRequestDto patientRequest) {
}
