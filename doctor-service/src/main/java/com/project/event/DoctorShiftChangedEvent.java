package com.project.event;

import java.time.Instant;
import java.util.UUID;

public record DoctorShiftChangedEvent(UUID doctorId, UUID shiftId, ShiftAction action, Instant occurredAt) {
    public enum ShiftAction { CREATED, DELETED, STATUS_CHANGED }
}
