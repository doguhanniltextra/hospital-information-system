package com.project.notification_service.repository;

import com.project.notification_service.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing notification templates.
 * Templates contain the subject, body, and delivery channel configurations.
 */
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    
    /**
     * Finds a notification template by its unique code.
     * 
     * @param templateCode The unique identifier for the template (e.g., 'LAB_RESULT_READY')
     * @return Optional containing the template if found
     */
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
}

