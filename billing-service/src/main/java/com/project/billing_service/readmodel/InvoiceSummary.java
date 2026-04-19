package com.project.billing_service.readmodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denormalized invoice summary optimized for fast retrieval.
 * This entity belongs to the Read Model in CQRS.
 */
@Entity
@Table(name = "invoice_summaries", schema = "billing_schema")
public class InvoiceSummary {

    @Id
    private UUID invoiceId;

    private UUID patientId;
    private UUID doctorId;
    private BigDecimal totalAmount;
    private BigDecimal patientOwes;
    private BigDecimal insuranceOwes;
    private String status; // e.g., "PAID", "PENDING", "CLAIM_SUBMITTED"

    // Denormalized data for performance
    private String patientName;
    private String doctorName;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Getters and setters

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPatientOwes() {
        return patientOwes;
    }

    public void setPatientOwes(BigDecimal patientOwes) {
        this.patientOwes = patientOwes;
    }

    public BigDecimal getInsuranceOwes() {
        return insuranceOwes;
    }

    public void setInsuranceOwes(BigDecimal insuranceOwes) {
        this.insuranceOwes = insuranceOwes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}