package com.project.support_service.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class InventoryAlertEvent {
    public String eventId;
    public UUID itemId;
    public String location;
    public Integer currentQuantity;
    public Integer threshold;
    public String alertType; // LOW_STOCK or EXPIRED
}
