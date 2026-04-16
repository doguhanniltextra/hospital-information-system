package com.project.inventory_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stocks", schema = "inventory_schema", uniqueConstraints = {@UniqueConstraint(columnNames = {"itemId", "location"})})
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer minThreshold;

    private LocalDateTime expiryDate;

    private String location; // e.g., Warehouse A, Floor 2 Shelf 3
    
    private LocalDateTime lastUpdatedAt;
    
    private LocalDateTime lastLowStockAlertAt;
    private LocalDateTime lastExpiredAlertAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Integer minThreshold) { this.minThreshold = minThreshold; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    public LocalDateTime getLastLowStockAlertAt() { return lastLowStockAlertAt; }
    public void setLastLowStockAlertAt(LocalDateTime lastLowStockAlertAt) { this.lastLowStockAlertAt = lastLowStockAlertAt; }
    public LocalDateTime getLastExpiredAlertAt() { return lastExpiredAlertAt; }
    public void setLastExpiredAlertAt(LocalDateTime lastExpiredAlertAt) { this.lastExpiredAlertAt = lastExpiredAlertAt; }
}
