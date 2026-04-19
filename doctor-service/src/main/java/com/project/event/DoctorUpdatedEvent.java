package com.project.event;

import com.project.model.Doctor;
import java.time.Instant;
import java.util.Set;

public record DoctorUpdatedEvent(Doctor doctor, Set<String> changedFields, Instant occurredAt) {
}
