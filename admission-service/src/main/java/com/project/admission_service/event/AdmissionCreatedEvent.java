package com.project.admission_service.event;

import com.project.admission_service.model.Admission;
import java.time.Instant;

public class AdmissionCreatedEvent {
    private final Admission admission;
    private final Instant occurredAt;

    public AdmissionCreatedEvent(Admission admission, Instant occurredAt) {
        this.admission = admission;
        this.occurredAt = occurredAt;
    }

    public Admission getAdmission() {
        return admission;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
