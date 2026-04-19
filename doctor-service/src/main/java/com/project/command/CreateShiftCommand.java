package com.project.command;

import com.project.dto.request.CreateShiftRequestDto;
import java.util.UUID;

public record CreateShiftCommand(UUID doctorId, CreateShiftRequestDto request) {
}
