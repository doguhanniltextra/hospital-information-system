package com.project.command;

import com.project.dto.request.CreateLeaveRequestDto;
import java.util.UUID;

public record CreateLeaveCommand(UUID doctorId, CreateLeaveRequestDto request) {
}
