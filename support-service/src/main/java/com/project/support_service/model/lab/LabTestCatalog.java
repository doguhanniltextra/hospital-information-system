package com.project.support_service.model.lab;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "lab_test_catalog", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabTestCatalog {
    @Id
    private String testCode;
    
    private String name;
    private String description;
    private BigDecimal price;
    private String department;
}
