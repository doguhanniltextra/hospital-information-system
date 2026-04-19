package com.project.billing_service.outbox;

import com.project.billing_service.model.BillingOutboxEvent;
import com.project.billing_service.repository.BillingOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BillingOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(BillingOutboxPublisher.class);

    private final BillingOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public BillingOutboxPublisher(BillingOutboxRepository outboxRepository,
                                  KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishEvents() {
        List<BillingOutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox: Found {} pending events in billing-service", pendingEvents.size());

        for (BillingOutboxEvent event : pendingEvents) {
            try {
                // Topic: billing-events.v1
                kafkaTemplate.send("billing-events.v1", event.getPayload());
                
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Outbox: Successfully published billing event {} (type: {})", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Outbox: Failed to publish billing event {}. Will retry.", event.getId(), e);
            }
        }
    }
}
