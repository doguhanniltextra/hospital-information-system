package com.project.support_service.repository;

import com.project.support_service.model.outbox.SupportProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupportProcessedEventRepository extends JpaRepository<SupportProcessedEvent, String> {
    Optional<SupportProcessedEvent> findByEventIdAndConsumerName(String eventId, String consumerName);
}
