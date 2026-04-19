package com.project.admission_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admission_processed_events")
public class AdmissionProcessedEvent {
    @Id
    private String messageId;
    private Instant processedAt;

    public AdmissionProcessedEvent() {}

    public AdmissionProcessedEvent(String messageId) {
        this.messageId = messageId;
        this.processedAt = Instant.now();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
