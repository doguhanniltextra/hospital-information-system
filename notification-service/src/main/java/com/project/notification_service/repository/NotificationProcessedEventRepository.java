package com.project.notification_service.repository;

import com.project.notification_service.model.NotificationProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for tracking processed Kafka messages to ensure idempotency.
 * Stores message identifiers that have already been handled by the consumer.
 */
public interface NotificationProcessedEventRepository extends JpaRepository<NotificationProcessedEvent, Long> {
    
    /**
     * Finds a processed event record by its unique Kafka message ID.
     * 
     * @param messageId The unique identifier of the Kafka message
     * @return Optional containing the record if found
     */
    Optional<NotificationProcessedEvent> findByMessageId(String messageId);

    /**
     * Checks if a message has already been processed.
     * 
     * @param messageId The unique identifier of the Kafka message
     * @return True if the message exists in the database
     */
    boolean existsByMessageId(String messageId);
}

