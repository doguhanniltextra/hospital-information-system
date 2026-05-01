package com.project.patient_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.patient_service.kafka.KafkaProducer;
import com.project.patient_service.model.PatientOutboxEvent;
import com.project.patient_service.repository.PatientOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PatientOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(PatientOutboxPublisher.class);

    private final PatientOutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    public PatientOutboxPublisher(PatientOutboxRepository outboxRepository,
                                  KafkaProducer kafkaProducer,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void publishEvents() {
        List<PatientOutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox: Found {} pending events to publish", pendingEvents.size());

        for (PatientOutboxEvent event : pendingEvents) {
            try {
                publishToKafka(event);
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Outbox: Successfully published event {} of type {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Outbox: Failed to publish event {}. Will retry later.", event.getId(), e);
                // We keep it PENDING to retry in the next cycle
            }
        }
    }

    private void publishToKafka(PatientOutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case "PATIENT_CREATED" -> kafkaProducer.sendRawEvent("patient-created.v1", event.getPayload());
            case "PATIENT_UPDATED" -> kafkaProducer.sendRawEvent("patient-updated.v1", event.getPayload());
            case "PATIENT_DELETED" -> kafkaProducer.sendDeleteEventAsync(java.util.UUID.fromString(event.getAggregateId()));
            default -> log.warn("Outbox: Unknown patient event type '{}' for event {}", event.getEventType(), event.getId());
        }
    }
}
