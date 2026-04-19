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

@Component
public class AppointmentOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(AppointmentOutboxPublisher.class);

    private final AppointmentOutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;

    public AppointmentOutboxPublisher(AppointmentOutboxRepository outboxRepository,
                                     KafkaProducer kafkaProducer) {
        this.outboxRepository = outboxRepository;
        this.kafkaProducer = kafkaProducer;
    }

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
