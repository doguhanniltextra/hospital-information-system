package com.project.billing_service.repository;

import com.project.billing_service.model.BillingOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BillingOutboxRepository extends JpaRepository<BillingOutboxEvent, UUID> {
    List<BillingOutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
}
