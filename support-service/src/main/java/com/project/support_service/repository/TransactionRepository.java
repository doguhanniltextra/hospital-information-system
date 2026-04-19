package com.project.support_service.repository;

import com.project.support_service.model.inventory.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<StockTransaction, UUID> {
    List<StockTransaction> findByItemId(UUID itemId);
    List<StockTransaction> findByReferenceId(String referenceId);
}
