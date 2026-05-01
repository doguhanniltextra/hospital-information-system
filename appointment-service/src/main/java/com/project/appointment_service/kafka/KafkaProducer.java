package com.project.appointment_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.constants.KafkaEndpoints;
import com.project.appointment_service.constants.LogMessages;
import com.project.appointment_service.dto.AppointmentKafkaResponseDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.stereotype.Service;

/**
 * Generic Kafka Producer for dispatching appointment-related events.
 * Handles serialization of DTOs to JSON and provides methods for both typed and raw event delivery.
 */
@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the KafkaProducer.
     * 
     * @param kafkaTemplate Spring Kafka template for low-level message dispatch
     */
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Dispatches a specific appointment response event.
     * 
     * @param appointment The appointment data to send
     */
    public void sendEvent(AppointmentKafkaResponseDto appointment) {
        try {
            String json = objectMapper.writeValueAsString(appointment);
            kafkaTemplate.send(KafkaEndpoints.KAFKA_SEND_EVENT_APPOINTMENT, json);
            log.info(LogMessages.KAFKA_SEND_EVENT_TRIGGERED);
        } catch (JsonProcessingException e) {
            log.error(LogMessages.KAFKA_SEND_EVENT_ERROR, e);
        }
    }

    /**
     * Dispatches a generic object payload to a specified Kafka topic.
     * 
     * @param topic The destination Kafka topic
     * @param payload The object to serialize and send
     */
    public void sendGenericEvent(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, json);
            log.info("KAFKA: Sent event to topic {}", topic);
        } catch (JsonProcessingException e) {
            log.error("KAFKA: Failed to serialize event for topic {}", topic, e);
            throw new RuntimeException("Kafka serialization failure", e);
        }
    }

    /**
     * Dispatches a pre-serialized JSON string to a specified Kafka topic.
     * 
     * @param topic The destination Kafka topic
     * @param json The JSON payload
     */
    public void sendRawEvent(String topic, String json) {
        kafkaTemplate.send(topic, json);
    }
}

