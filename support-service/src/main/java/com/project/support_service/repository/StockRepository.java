package com.project.support_service.repository;

import com.project.support_service.model.inventory.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {
    List<Stock> findByItemId(UUID itemId);
    List<Stock> findByLocation(String location);
    List<Stock> findByExpiryDateBefore(LocalDateTime date);
    java.util.Optional<Stock> findByItemIdAndLocation(UUID itemId, String location);
}
