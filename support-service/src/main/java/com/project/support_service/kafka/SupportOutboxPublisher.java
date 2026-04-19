package com.project.support_service.kafka;

import com.project.support_service.model.outbox.SupportOutboxEvent;
import com.project.support_service.repository.SupportOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SupportOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(SupportOutboxPublisher.class);
    
    private final SupportOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SupportOutboxPublisher(SupportOutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @Scheduled(fixedDelay = 3000)
    public void publishPendingEvents() {
        List<SupportOutboxEvent> pending = outboxRepository.claimPending(Instant.now());
        if (pending.isEmpty()) return;

        log.info("Processing {} pending support outbox events", pending.size());

        for (SupportOutboxEvent event : pending) {
            try {
                event.setStatus("PROCESSING");
                outboxRepository.save(event);

                // Use eventType as the topic name or map it
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayloadJson());

                event.setStatus("SENT");
                outboxRepository.save(event);
                log.info("Published support event: {} to topic: {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish support event: {}", event.getId(), e);
                int retry = event.getRetryCount() + 1;
                event.setRetryCount(retry);
                event.setStatus(retry >= 10 ? "FAILED" : "PENDING");
                event.setNextRetryAt(Instant.now().plusSeconds((long) Math.pow(2, Math.min(retry, 8))));
                outboxRepository.save(event);
            }
        }
    }
}
