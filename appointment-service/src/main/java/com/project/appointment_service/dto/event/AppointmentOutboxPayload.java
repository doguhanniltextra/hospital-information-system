package com.project.appointment_service.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public class AppointmentOutboxPayload {
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String patientEmail;
    private BigDecimal amount;
    private String status;
    private String action; // e.g., "CREATED", "PAID", "CANCELLED"
    private long timestamp;

    public AppointmentOutboxPayload() {}

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
