package com.project.support_service.model.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_outbox_event", schema = "support_schema")
@Data
@NoArgsConstructor
public class SupportOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String payloadJson;
    
    private String status;
    private Integer retryCount;
    private Instant nextRetryAt;
    private Instant createdAt;

    public SupportOutboxEvent(String aggregateType, String aggregateId, String eventType, String payloadJson) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
        this.status = "PENDING";
        this.retryCount = 0;
    }
}
