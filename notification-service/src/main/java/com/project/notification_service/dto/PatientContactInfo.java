package com.project.notification_service.dto;

import java.io.Serializable;

public record PatientContactInfo(
    String id,
    String name,
    String email,
    String phoneNumber,
    String address
) implements Serializable {}
