package com.project.lab_service.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_order", schema = "lab_schema")
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

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }
    public LabOrderStatus getStatus() { return status; }
    public void setStatus(LabOrderStatus status) { this.status = status; }
    public PriorityLevel getPriority() { return priority; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
