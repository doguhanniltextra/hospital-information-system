package com.project.inventory_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_transactions", schema = "inventory_schema")
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private Integer quantityChange; // +10 (replenish), -1 (consume)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String reason; // e.g., Patient Consumption, Supply Order, Expiry Adjustment

    private UUID referenceId; // Correlation with item-consumed event or order ID

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this. type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
