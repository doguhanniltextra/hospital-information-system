package com.project.patient_service.readmodel;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.project.patient_service.security.CryptoConverter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Denormalized patient summary optimized for fast retrieval.
 * This entity belongs to the Read Model in CQRS.
 */
@Entity
@Table(name = "patient_summaries", schema = "patient_schema")
public class PatientSummary {

    @Id
    private UUID id;

    @Convert(converter = CryptoConverter.class)
    private String name;
    private String email;
    
    @Convert(converter = CryptoConverter.class)
    private String phoneNumber;
    
    // Denormalized/Flattened Insurance Data
    private String insuranceProviderName;
    
    @Convert(converter = CryptoConverter.class)
    private String insurancePolicyNumber;

    private LocalDateTime lastUpdated;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getInsuranceProviderName() {
        return insuranceProviderName;
    }

    public void setInsuranceProviderName(String insuranceProviderName) {
        this.insuranceProviderName = insuranceProviderName;
    }

    public String getInsurancePolicyNumber() {
        return insurancePolicyNumber;
    }

    public void setInsurancePolicyNumber(String insurancePolicyNumber) {
        this.insurancePolicyNumber = insurancePolicyNumber;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
