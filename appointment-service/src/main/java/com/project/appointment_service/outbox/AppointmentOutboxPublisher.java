package com.project.appointment_service.outbox;

import com.project.appointment_service.kafka.KafkaProducer;
import com.project.appointment_service.model.AppointmentOutboxEvent;
import com.project.appointment_service.repository.AppointmentOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background scheduler that implements the "Transactional Outbox" pattern.
 * Periodically polls the database for pending appointment events and publishes them to Kafka.
 * This ensures "At Least Once" delivery guarantee for asynchronous service integrations.
 */
@Component
public class AppointmentOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(AppointmentOutboxPublisher.class);

    private final AppointmentOutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;

    /**
     * Initializes the AppointmentOutboxPublisher.
     * 
     * @param outboxRepository Repository to query pending events
     * @param kafkaProducer Producer to dispatch events to Kafka
     */
    public AppointmentOutboxPublisher(AppointmentOutboxRepository outboxRepository,
                                     KafkaProducer kafkaProducer) {
        this.outboxRepository = outboxRepository;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Scheduled task that runs every 5 seconds.
     * 1. Fetches all PENDING events from the outbox table.
     * 2. Maps events to their respective Kafka topics based on event type.
     * 3. Dispatches payload to Kafka via {@link KafkaProducer}.
     * 4. Marks events as PROCESSED upon successful dispatch.
     * 
     * Topics handled:
     * - APPOINTMENT_CREATED -> doctor-patient-count-update.v1
     * - PAYMENT_UPDATE -> appointment-payment-updates.v1
     * - DEFAULT -> appointment-events.v1
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishEvents() {
        List<AppointmentOutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox: Found {} pending events in appointment-service", pendingEvents.size());

        for (AppointmentOutboxEvent event : pendingEvents) {
            try {
                String topic;
                if ("APPOINTMENT_CREATED".equals(event.getEventType())) {
                    topic = "doctor-patient-count-update.v1";
                } else if ("PAYMENT_UPDATE".equals(event.getEventType())) {
                    topic = "appointment-payment-updates.v1";
                } else {
                    topic = "appointment-events.v1";
                }

                kafkaProducer.sendRawEvent(topic, event.getPayload());
                
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Outbox: Successfully published {} event to topic {}", event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Outbox: Failed to publish appointment event {}. Will retry.", event.getId(), e);
            }
        }
    }
}
