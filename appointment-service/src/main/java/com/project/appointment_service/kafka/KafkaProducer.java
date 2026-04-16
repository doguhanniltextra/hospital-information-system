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

@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(AppointmentKafkaResponseDto appointment) {
        try {
            String json = objectMapper.writeValueAsString(appointment);
            kafkaTemplate.send(KafkaEndpoints.KAFKA_SEND_EVENT_APPOINTMENT, json);
            log.info(LogMessages.KAFKA_SEND_EVENT_TRIGGERED);
        } catch (JsonProcessingException e) {
            log.error(LogMessages.KAFKA_SEND_EVENT_ERROR, e);
        }
    }
}

