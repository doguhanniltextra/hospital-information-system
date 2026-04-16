package com.project.lab_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LabOrderPlacedEvent {
    public String eventId;
    public String eventVersion;
    public Instant occurredAt;
    public UUID orderId;
    public UUID patientId;
    public String patientEmail;
    public String patientPhone;
    public UUID doctorId;
    public List<TestItem> tests;
    public BigDecimal orderTotal;
    public String priority;
    public String correlationId;

    public static class TestItem {
        public String testCode;
        public String testName;
        public BigDecimal unitPrice;
        public String currency;
        public Integer quantity;
        public BigDecimal lineTotal;
    }
}
