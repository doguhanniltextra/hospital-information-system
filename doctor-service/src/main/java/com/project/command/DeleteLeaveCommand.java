package com.project.command;

import java.util.UUID;

public record DeleteLeaveCommand(UUID doctorId, UUID leaveId) {
}
