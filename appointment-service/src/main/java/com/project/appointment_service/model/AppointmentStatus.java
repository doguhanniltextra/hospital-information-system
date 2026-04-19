package com.project.appointment_service.model;

public enum AppointmentStatus {
    PENDING_CONFIRMATION,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    ADMISSION_PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    REJECTED_SYSTEM_ERROR
}
