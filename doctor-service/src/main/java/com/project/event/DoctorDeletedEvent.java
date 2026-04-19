package com.project.event;

import java.time.Instant;
import java.util.UUID;

public record DoctorDeletedEvent(UUID doctorId, Instant occurredAt) {
}
