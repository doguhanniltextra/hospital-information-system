package com.project.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.constants.KafkaTopics;
import com.project.event.DoctorCreatedEvent;
import com.project.event.DoctorDeletedEvent;
import com.project.event.DoctorUpdatedEvent;
import com.project.model.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DoctorEventKafkaPublisher {
    private static final Logger log = LoggerFactory.getLogger(DoctorEventKafkaPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DoctorEventKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    public void handleDoctorCreated(DoctorCreatedEvent event) {
        publishDoctorEvent(event.doctor(), "DOCTOR_CREATED");
    }

    @Async
    @EventListener
    public void handleDoctorUpdated(DoctorUpdatedEvent event) {
        publishDoctorEvent(event.doctor(), "DOCTOR_UPDATED");
    }

    @Async
    @EventListener
    public void handleDoctorDeleted(DoctorDeletedEvent event) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("doctorId", event.id().toString());
            payload.put("eventType", "DOCTOR_DELETED");

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(KafkaTopics.DOCTOR_UPDATED, event.id().toString(), json);
            log.info("Published DOCTOR_DELETED event for doctor {}", event.id());
        } catch (Exception e) {
            log.error("Failed to publish DOCTOR_DELETED event for doctor {}", event.id(), e);
        }
    }

    private void publishDoctorEvent(Doctor doctor, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("doctorId", doctor.getId().toString());
            payload.put("name", doctor.getName());
            payload.put("specialization", doctor.getSpecialization());
            payload.put("eventType", eventType);

            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(KafkaTopics.DOCTOR_UPDATED, doctor.getId().toString(), json);
            log.info("Published {} event for doctor {}", eventType, doctor.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event for doctor {}", eventType, doctor.getId(), e);
        }
    }
}
