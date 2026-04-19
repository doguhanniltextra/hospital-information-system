package com.project.notification_service.dto;

import java.util.UUID;

public class AppointmentScheduledEvent {
    public UUID patientId;
    public UUID doctorId;
    public String appointmentDate;
    public double amount;
}
