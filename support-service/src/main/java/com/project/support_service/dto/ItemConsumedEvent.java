package com.project.support_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ItemConsumedEvent {
    public String eventId;
    public UUID itemId;
    public Integer quantity;
    public LocalDateTime occurredAt;
    public String correlationId;
}
