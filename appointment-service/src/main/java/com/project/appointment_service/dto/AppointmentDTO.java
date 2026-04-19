package com.project.appointment_service.dto;

import java.util.UUID;

import com.project.appointment_service.model.PaymentType;
import com.project.appointment_service.model.ServiceType;

public class AppointmentDTO {

    public String serviceDate;
    public ServiceType serviceType;
    public float amount;
    public PaymentType paymentType;
    public boolean paymentStatus;
    public String status;



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
    public PaymentType getPaymentType() {
        return paymentType;
    }
    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }
    public boolean isPaymentStatus() {
        return paymentStatus;
    }
    public void setPaymentStatus(boolean paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    
}   
