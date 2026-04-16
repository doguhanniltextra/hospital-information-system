package com.project.appointment_service.dto;

public class AppointmentKafkaResponseDto {
    private String doctorId;
    private String patientId;
    private String patientEmail;
    private String patientPhone;
    private double amount;
    private boolean paymentStatus;
    private String insuranceProviderType;
    private String providerName;

    public AppointmentKafkaResponseDto(String doctorId, String patientId, String patientEmail, String patientPhone, double amount, boolean paymentStatus, String insuranceProviderType, String providerName) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.insuranceProviderType = insuranceProviderType;
        this.providerName = providerName;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public boolean isPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(boolean paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getInsuranceProviderType() { return insuranceProviderType; }
    public void setInsuranceProviderType(String insuranceProviderType) { this.insuranceProviderType = insuranceProviderType; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
}
