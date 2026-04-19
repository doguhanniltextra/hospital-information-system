package com.project.patient_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.patient_service.dto.request.KafkaPatientRequestDto;
import com.project.patient_service.helper.UserValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserValidator userValidator;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, UserValidator userValidator) {
        this.kafkaTemplate = kafkaTemplate;
        this.userValidator = userValidator;
    }

    /**
     * Async fire-and-forget: sends PATIENT_CREATED event without blocking the request thread.
     * Success/failure is logged via CompletableFuture callback.
     */
    public void sendEventAsync(KafkaPatientRequestDto kafkaPatientRequestDto) {
        try {
            Map<String, Object> event = userValidator.getStringObjectMap(kafkaPatientRequestDto);
            String json = objectMapper.writeValueAsString(event);

            CompletableFuture<?> future = kafkaTemplate.send("patient-updated.v1", json);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("KAFKA: Failed to send PATIENT_CREATED event for patientId={}",
                            kafkaPatientRequestDto.getId(), ex);
                } else {
                    log.info("KAFKA: Sent PATIENT_CREATED event for patientId={}", kafkaPatientRequestDto.getId());
                }
            });
        } catch (Exception e) {
            log.error("KAFKA: Failed to serialize PATIENT_CREATED event", e);
        }
    }

    /**
     * Async fire-and-forget: sends PATIENT_DELETED event without blocking the request thread.
     */
    public void sendDeleteEventAsync(UUID patientId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("patientId", patientId.toString());
            event.put("eventType", EventType.PATIENT_DELETED.name());

            String json = objectMapper.writeValueAsString(event);

            CompletableFuture<?> future = kafkaTemplate.send("patient-updated.v1", json);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("KAFKA: Failed to send PATIENT_DELETED event for patientId={}", patientId, ex);
                } else {
                    log.info("KAFKA: Sent PATIENT_DELETED event for patientId={}", patientId);
                }
            });
        } catch (Exception e) {
            log.error("KAFKA: Failed to serialize PATIENT_DELETED event for patientId={}", patientId, e);
        }
    }

    // Keep legacy sync methods for backward compatibility
    public void sendEvent(KafkaPatientRequestDto kafkaPatientRequestDto) {
        sendEventAsync(kafkaPatientRequestDto);
    }

    public void sendDeleteEvent(UUID patientId) {
        sendDeleteEventAsync(patientId);
    }

    public void sendRawEvent(String topic, String json) {
        kafkaTemplate.send(topic, json);
    }
}
