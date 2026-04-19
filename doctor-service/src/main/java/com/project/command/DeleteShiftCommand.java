package com.project.command;

import java.util.UUID;

public record DeleteShiftCommand(UUID doctorId, UUID shiftId) {
}
