package com.project.patient_service.dto.request;

import java.util.UUID;

public class KafkaPatientRequestDto {
    UUID Id;

    String name;
    private String email;
    private String phoneNumber;
    String eventType;


    public UUID getId() {
        return Id;
    }

    public void setId(UUID id) {
        Id = id;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
