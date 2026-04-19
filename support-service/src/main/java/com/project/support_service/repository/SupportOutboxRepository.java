package com.project.support_service.repository;

import com.project.support_service.model.outbox.SupportOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SupportOutboxRepository extends JpaRepository<SupportOutboxEvent, UUID> {
    
    @Query("SELECT e FROM SupportOutboxEvent e WHERE e.status = 'PENDING' OR (e.status = 'PROCESSING' AND e.nextRetryAt <= :now) ORDER BY e.createdAt ASC")
    List<SupportOutboxEvent> claimPending(@Param("now") Instant now);
    
    List<SupportOutboxEvent> findByStatus(String status);
}
