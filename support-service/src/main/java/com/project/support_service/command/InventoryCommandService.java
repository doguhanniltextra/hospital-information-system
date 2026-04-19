package com.project.support_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.support_service.dto.InventoryAlertEvent;
import com.project.support_service.model.inventory.Stock;
import com.project.support_service.model.inventory.StockTransaction;
import com.project.support_service.model.inventory.TransactionType;
import com.project.support_service.model.outbox.SupportOutboxEvent;
import com.project.support_service.repository.StockRepository;
import com.project.support_service.repository.SupportOutboxRepository;
import com.project.support_service.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryCommandService {
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;
    private final SupportOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryCommandService(StockRepository stockRepository,
                                   TransactionRepository transactionRepository,
                                   SupportOutboxRepository outboxRepository,
                                   ObjectMapper objectMapper) {
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(value = "inventoryStocks", key = "#itemId")
    public void consumeItem(UUID itemId, Integer quantity, UUID referenceId) {
        List<Stock> stocks = stockRepository.findByItemId(itemId);
        if (stocks.isEmpty()) {
            throw new RuntimeException("No stock records found for item: " + itemId);
        }

        int remainingToConsume = quantity;
        for (Stock stock : stocks) {
            if (stock.getQuantity() > 0) {
                int consumedFromThis = Math.min(stock.getQuantity(), remainingToConsume);
                stock.setQuantity(stock.getQuantity() - consumedFromThis);
                stockRepository.save(stock);
                
                remainingToConsume -= consumedFromThis;
                
                StockTransaction tx = new StockTransaction();
                tx.setItemId(itemId);
                tx.setQuantityChange(-consumedFromThis);
                tx.setType(TransactionType.CONSUMPTION);
                tx.setReason("Consumption logic for reference: " + referenceId);
                tx.setReferenceId(referenceId.toString());
                tx.setOccurredAt(LocalDateTime.now());
                transactionRepository.save(tx);
                
                checkThresholdAndAlert(stock);

                if (remainingToConsume == 0) break;
            }
        }

        if (remainingToConsume > 0) {
            // Handle deficit
            Stock firstLocation = stocks.get(0);
            firstLocation.setQuantity(firstLocation.getQuantity() - remainingToConsume);
            stockRepository.save(firstLocation);

            StockTransaction tx = new StockTransaction();
            tx.setItemId(itemId);
            tx.setQuantityChange(-remainingToConsume);
            tx.setType(TransactionType.CONSUMPTION);
            tx.setReason("Forced consumption deficit for reference: " + referenceId);
            tx.setReferenceId(referenceId.toString());
            tx.setOccurredAt(LocalDateTime.now());
            transactionRepository.save(tx);
            
            checkThresholdAndAlert(firstLocation);
        }
    }

    @Transactional
    @CacheEvict(value = "inventoryStocks", key = "#itemId")
    public Stock restock(UUID itemId, String location, Integer quantity, LocalDateTime expiryDate) {
        Stock stock = stockRepository.findByItemIdAndLocation(itemId, location).orElseGet(() -> {
            Stock s = new Stock();
            s.setItemId(itemId);
            s.setLocation(location);
            s.setQuantity(0);
            s.setMinThreshold(10);
            return s;
        });

        stock.setQuantity(stock.getQuantity() + quantity);
        if (expiryDate != null) stock.setExpiryDate(expiryDate);
        Stock saved = stockRepository.save(stock);

        StockTransaction tx = new StockTransaction();
        tx.setItemId(itemId);
        tx.setQuantityChange(quantity);
        tx.setType(TransactionType.RESTOCK);
        tx.setReason("Restock to " + location);
        tx.setOccurredAt(LocalDateTime.now());
        transactionRepository.save(tx);

        return saved;
    }

    private void checkThresholdAndAlert(Stock stock) {
        if (stock.getQuantity() <= stock.getMinThreshold()) {
            emitAlert("inventory-low-stock.v1", "LOW_STOCK", stock);
        }
    }

    private void emitAlert(String topic, String alertType, Stock stock) {
        InventoryAlertEvent event = new InventoryAlertEvent();
        event.eventId = UUID.randomUUID().toString();
        event.itemId = stock.getItemId();
        event.location = stock.getLocation();
        event.currentQuantity = stock.getQuantity();
        event.threshold = stock.getMinThreshold();
        event.alertType = alertType;

        try {
            SupportOutboxEvent ob = new SupportOutboxEvent(
                    "STOCK",
                    stock.getItemId().toString(),
                    topic,
                    objectMapper.writeValueAsString(event)
            );
            outboxRepository.save(ob);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize inventory alert", e);
        }
    }
}
