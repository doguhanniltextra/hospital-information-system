package com.project.support_service.dto;

import lombok.Data;
import java.util.UUID;
import java.time.Instant;

@Data
public class PatientUpdatedEvent {
    public String eventId;
    public UUID id; // patientId
    public String authUserId;
    public String eventType;
    public Instant occurredAt;
}
