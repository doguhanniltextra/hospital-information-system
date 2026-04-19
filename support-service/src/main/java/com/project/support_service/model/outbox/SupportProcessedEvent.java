package com.project.support_service.model.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_processed_event", schema = "support_schema")
@Data
@NoArgsConstructor
public class SupportProcessedEvent {
    @Id
    private String eventId;
    private String consumerName;
    private Instant processedAt;

    public SupportProcessedEvent(String eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.processedAt = Instant.now();
    }
}
