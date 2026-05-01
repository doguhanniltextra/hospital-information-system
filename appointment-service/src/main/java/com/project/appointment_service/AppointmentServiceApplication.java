package com.project.appointment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main entry point for the Appointment Service.
 * This service manages the lifecycle of clinical appointments, including
 * scheduling, status tracking, and multi-service saga orchestration for creation.
 */
@SpringBootApplication
@EnableKafka
public class AppointmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}

