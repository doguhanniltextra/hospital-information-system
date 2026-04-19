package com.project.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.command.DoctorCommandService;
import com.project.command.IncreasePatientNumberCommand;
import com.project.exception.DoctorNotFoundException;
import com.project.exception.PatientLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DoctorPatientCountConsumer {
    private static final Logger log = LoggerFactory.getLogger(DoctorPatientCountConsumer.class);
    private final DoctorCommandService doctorCommandService;
    private final ObjectMapper objectMapper;

    public DoctorPatientCountConsumer(DoctorCommandService doctorCommandService, ObjectMapper objectMapper) {
        this.doctorCommandService = doctorCommandService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "doctor-patient-count-update.v1", groupId = "doctor-service-group")
    public void handlePatientCountIncrement(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String doctorIdStr = node.get("doctorId").asText();
            UUID doctorId = UUID.fromString(doctorIdStr);
            
            log.info("KAFKA: Received increment request for doctor {}", doctorId);
            
            doctorCommandService.increasePatientNumber(new IncreasePatientNumberCommand(doctorId));
            log.info("KAFKA: Successfully incremented patient count for doctor {}", doctorId);
            
        } catch (DoctorNotFoundException e) {
            log.error("KAFKA: Doctor not found for increment: {}", e.getMessage());
        } catch (PatientLimitException e) {
            log.warn("KAFKA: Patient limit reached for doctor: {}", e.getMessage());
        } catch (Exception e) {
            log.error("KAFKA: Unexpected error processing increment event", e);
        }
    }
}
