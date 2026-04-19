package com.project.event;

import com.project.model.Doctor;
import java.time.Instant;

public record DoctorCreatedEvent(Doctor doctor, Instant occurredAt) {
}
