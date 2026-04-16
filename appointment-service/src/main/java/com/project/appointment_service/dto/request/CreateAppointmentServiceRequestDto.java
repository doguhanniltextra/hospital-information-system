package com.project.appointment_service.dto.request;

import com.project.appointment_service.model.PaymentType;
import com.project.appointment_service.model.ServiceType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateAppointmentServiceRequestDto {

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


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(String serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getServiceDateEnd() {
        return serviceDateEnd;
    }

    public void setServiceDateEnd(String serviceDateEnd) {
        this.serviceDateEnd = serviceDateEnd;
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
