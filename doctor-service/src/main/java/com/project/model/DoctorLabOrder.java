package com.project.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_lab_orders", schema = "doctor_schema")
public class DoctorLabOrder {
    @Id
    private UUID orderId;
    private UUID doctorId;
    private UUID patientId;
    private String status;
    @Enumerated(EnumType.STRING)
    private PriorityLevel priority;
    private Instant requestedAt;
    private BigDecimal orderTotal;

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public PriorityLevel getPriority() { return priority; }
    public void setPriority(PriorityLevel priority) { this.priority = priority; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public BigDecimal getOrderTotal() { return orderTotal; }
    public void setOrderTotal(BigDecimal orderTotal) { this.orderTotal = orderTotal; }
}
