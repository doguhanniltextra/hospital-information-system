package com.project.support_service.model.inventory;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "stocks", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID itemId;
    private Integer quantity;
    private String location;
    private LocalDateTime expiryDate;
    private Integer minThreshold;
    
    @Version
    private Long version;
}
