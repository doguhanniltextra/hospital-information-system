package com.project.inventory_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class RestockRequest {
    private UUID itemId;
    private String location;
    private Integer quantity;
    private LocalDateTime expiryDate;

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
}
