package com.project.support_service.model.lab;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "test_result", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID orderId;
    private String testCode;
    private String value;
    private String unit;
    private String referenceRange;
    private String abnormalFlag; // N, H, L
    private String reportPdfUrl;
    private Instant validatedAt;
}
