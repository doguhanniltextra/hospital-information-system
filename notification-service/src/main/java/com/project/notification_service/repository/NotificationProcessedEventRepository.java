package com.project.notification_service.repository;

import com.project.notification_service.model.NotificationProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationProcessedEventRepository extends JpaRepository<NotificationProcessedEvent, Long> {
    Optional<NotificationProcessedEvent> findByMessageId(String messageId);
    boolean existsByMessageId(String messageId);
}
