package com.project.appointment_service.model;


import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "appointments", schema = "appointment_schema")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    private UUID patientId;
    @NotNull
    private UUID doctorId;
    @NotNull
    private String serviceDate;
    @NotNull
    private String serviceDateEnd;
    @NotNull
    private ServiceType serviceType;
    @NotNull
    private float amount;
    @NotNull
    private boolean paymentStatus;
    @NotNull
    private PaymentType paymentType;


    public String getServiceDateEnd() {
        return serviceDateEnd;
    }

    public void setServiceDateEnd(String serviceDateEnd) {
        this.serviceDateEnd = serviceDateEnd;
    }

    public @NotNull UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(@NotNull UUID patientId) {
        this.patientId = patientId;
    }

    public @NotNull UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(@NotNull UUID doctorId) {
        this.doctorId = doctorId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }


    public String getServiceDate() {
        return serviceDate;
    }
    public void setServiceDate(String serviceDate) {
        this.serviceDate = serviceDate;
    }
    public ServiceType getServiceType() {
        return serviceType;
    }
    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
    public float getAmount() {
        return amount;
    }
    public void setAmount(float amount) {
        this.amount = amount;
    }
    public boolean isPaymentStatus() {
        return paymentStatus;
    }
    public void setPaymentStatus(boolean paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    public PaymentType getPaymentType() {
        return paymentType;
    }
    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

} 