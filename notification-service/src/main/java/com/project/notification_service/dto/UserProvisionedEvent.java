package com.project.notification_service.dto;

/**
 * Event DTO published by auth-service on the user-provisioned.v1 Kafka topic.
 * Contains the data needed to send the patient welcome email with a password reset link.
 */
public class UserProvisionedEvent {

    private String eventId;
    private String patientId;
    private String authUserId;
    private String email;
    private String name;
    private String resetToken;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAuthUserId() { return authUserId; }
    public void setAuthUserId(String authUserId) { this.authUserId = authUserId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
}
