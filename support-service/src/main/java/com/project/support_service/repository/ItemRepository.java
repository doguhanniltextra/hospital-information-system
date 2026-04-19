package com.project.support_service.repository;

import com.project.support_service.model.inventory.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    Optional<Item> findBySku(String sku);
}
