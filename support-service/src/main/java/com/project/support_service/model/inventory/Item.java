package com.project.support_service.model.inventory;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "items", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String sku;
    private String category; // Lab, General, etc.
    private String unitOfMeasure;
    private Integer minThreshold;
}
