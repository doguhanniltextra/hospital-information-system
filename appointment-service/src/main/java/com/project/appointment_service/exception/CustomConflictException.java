package com.project.appointment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CustomConflictException extends RuntimeException {
    public CustomConflictException(String message) {
        super(message);
    }
}
