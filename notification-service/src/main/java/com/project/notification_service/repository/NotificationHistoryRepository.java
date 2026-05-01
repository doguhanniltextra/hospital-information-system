package com.project.notification_service.repository;

import com.project.notification_service.model.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Repository for tracking the history of sent notifications.
 * Stores information about recipients, channels, and delivery status.
 */
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, UUID> {
}

