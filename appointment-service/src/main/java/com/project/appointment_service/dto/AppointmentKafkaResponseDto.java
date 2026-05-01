package com.project.appointment_service.dto;

import java.math.BigDecimal;

public class AppointmentKafkaResponseDto {
    private String doctorId;
    private String patientId;
    private BigDecimal amount;
    private boolean paymentStatus;
    private String insuranceProviderType;
    private String providerName;

    public AppointmentKafkaResponseDto(String doctorId, String patientId, BigDecimal amount, boolean paymentStatus,
            String insuranceProviderType, String providerName) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.insuranceProviderType = insuranceProviderType;
        this.providerName = providerName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(boolean paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getInsuranceProviderType() {
        return insuranceProviderType;
    }

    public void setInsuranceProviderType(String insuranceProviderType) {
        this.insuranceProviderType = insuranceProviderType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
