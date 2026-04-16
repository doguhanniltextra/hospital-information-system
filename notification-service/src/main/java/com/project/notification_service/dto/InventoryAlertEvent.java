package com.project.notification_service.dto;

import java.util.UUID;

public class InventoryAlertEvent {
    public String eventId;
    public UUID itemId;
    public String location;
    public Integer currentQuantity;
    public Integer threshold;
    public String alertType; // LOW_STOCK or EXPIRED
}
