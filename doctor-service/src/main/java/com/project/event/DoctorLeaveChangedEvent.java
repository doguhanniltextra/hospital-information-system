package com.project.event;

import java.time.Instant;
import java.util.UUID;

public record DoctorLeaveChangedEvent(UUID doctorId, UUID leaveId, LeaveAction action, Instant occurredAt) {
    public enum LeaveAction { CREATED, APPROVED, CANCELLED }
}
