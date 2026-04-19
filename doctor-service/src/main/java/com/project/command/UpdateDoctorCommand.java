package com.project.command;

import com.project.dto.UpdateDoctorServiceRequestDto;
import java.util.UUID;

public record UpdateDoctorCommand(UUID id, UpdateDoctorServiceRequestDto request) {
}
