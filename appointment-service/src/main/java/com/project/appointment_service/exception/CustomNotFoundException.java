package com.project.appointment_service.exception;


public class CustomNotFoundException extends RuntimeException  {
    public CustomNotFoundException(String message) {
        super(message);
    }
}

