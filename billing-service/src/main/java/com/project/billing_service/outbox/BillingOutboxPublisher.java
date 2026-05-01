package com.project.billing_service.outbox;

import com.project.billing_service.constants.KafkaTopics;
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

    /**
     * After MAX_RETRY_ATTEMPTS consecutive Kafka failures the event is marked FAILED
     * so it does not loop forever. A separate recovery job or manual intervention
     * is needed to replay FAILED events.
     */
    private static final int MAX_RETRY_ATTEMPTS = 5;

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

            // Guard: abandon events that have exceeded the retry budget
            if (event.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                log.error("Outbox: Event {} (type: {}) exceeded {} retry attempts. Marking FAILED — manual intervention required.",
                        event.getId(), event.getEventType(), MAX_RETRY_ATTEMPTS);
                event.setStatus("FAILED");
                outboxRepository.save(event);
                continue;
            }

            try {
                kafkaTemplate.send(KafkaTopics.BILLING_EVENTS, event.getPayload());

                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Outbox: Successfully published billing event {} (type: {})", event.getId(), event.getEventType());
            } catch (Exception e) {
                int nextRetry = event.getRetryCount() + 1;
                log.error("Outbox: Failed to publish billing event {} — attempt {}/{}. Will retry.",
                        event.getId(), nextRetry, MAX_RETRY_ATTEMPTS, e);
                event.setRetryCount(nextRetry);
                outboxRepository.save(event);
            }
        }
    }
}
