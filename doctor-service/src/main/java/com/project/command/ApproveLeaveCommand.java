package com.project.command;

import java.util.UUID;

public record ApproveLeaveCommand(UUID doctorId, UUID leaveId) {
}
