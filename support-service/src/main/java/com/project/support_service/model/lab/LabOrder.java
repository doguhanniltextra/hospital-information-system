package com.project.support_service.model.lab;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "lab_order", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrder {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID orderId;
    
    private UUID patientId;
    private String patientEmail;
    private String patientPhone;
    private UUID doctorId;
    
    @Enumerated(EnumType.STRING)
    private LabOrderStatus status;
    
    @Enumerated(EnumType.ORDINAL)
    private PriorityLevel priority;
    
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;
    private BigDecimal totalAmount;
}
