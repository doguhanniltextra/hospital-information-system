package com.project.service;

import com.project.constants.KafkaTopics;
import com.project.model.DoctorOutboxEvent;
import com.project.repository.DoctorOutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Component responsible for relaying events from the doctor-service outbox table to Kafka.
 * Polling is implemented via a scheduled task with exponential backoff on failure.
 */
@Service
public class DoctorOutboxPublisher {
    private final DoctorOutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${kafka.topics.lab-order-placed:lab-order-placed.v1}")
    private String labOrderPlacedTopic;

    /**
     * Initializes the publisher with repository and Kafka template.
     * 
     * @param outboxEventRepository Repository for managing the outbox table
     * @param kafkaTemplate Spring Kafka template for message dispatch
     */
    public DoctorOutboxPublisher(DoctorOutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Scheduled task to fetch and publish pending outbox events.
     * Events are claimed to avoid concurrent processing, published to Kafka, and marked as SENT.
     * Implements exponential backoff for retries and fails permanently after 10 attempts.
     */
    @Transactional
    @Scheduled(fixedDelayString = "${doctor.outbox.publisher.delay-ms:3000}")
    public void publishPendingEvents() {
        List<DoctorOutboxEvent> pending = outboxEventRepository.claimPending(Instant.now());
        for (DoctorOutboxEvent event : pending) {
            try {
                event.setStatus("PROCESSING");
                outboxEventRepository.save(event);
                kafkaTemplate.send(labOrderPlacedTopic, event.getAggregateId(), event.getPayloadJson());
                event.setStatus("SENT");
                outboxEventRepository.save(event);
            } catch (Exception e) {
                int retry = event.getRetryCount() + 1;
                event.setRetryCount(retry);
                event.setStatus(retry >= 10 ? "FAILED" : "PENDING");
                event.setNextRetryAt(Instant.now().plusSeconds((long) Math.pow(2, Math.min(retry, 8))));
                outboxEventRepository.save(event);
            }
        }
    }
}
