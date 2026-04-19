package com.project.support_service.model.inventory;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "stock_transactions", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID itemId;
    private Integer quantityChange;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    private String reason;
    private LocalDateTime occurredAt;
    private String referenceId; // e.g. Lab Order ID or Event ID
}
