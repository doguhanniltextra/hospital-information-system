package com.project.inventory_service.repository;

import com.project.inventory_service.model.InventoryOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InventoryOutboxRepository extends JpaRepository<InventoryOutboxEvent, UUID> {
}
