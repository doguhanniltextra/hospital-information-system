package com.project.inventory_service.service;

import com.project.inventory_service.model.*;
import com.project.inventory_service.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.project.inventory_service.dto.InventoryAlertEvent;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;
    private final InventoryOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(ItemRepository itemRepository, 
                            StockRepository stockRepository, 
                            TransactionRepository transactionRepository,
                            InventoryOutboxRepository outboxRepository,
                            ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
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
                tx.setReason("Patient Consumption from " + stock.getLocation());
                tx.setReferenceId(referenceId);
                transactionRepository.save(tx);
                
                if (stock.getQuantity() <= stock.getMinThreshold()) {
                    log.error("CRITICAL: Item {} stock at {} ({}) is below threshold ({})!", 
                              itemId, stock.getLocation(), stock.getQuantity(), stock.getMinThreshold());
                    
                    // Discard duplicate alerts for today
                    boolean shouldAlert = stock.getLastLowStockAlertAt() == null || stock.getLastLowStockAlertAt().isBefore(LocalDate.now().atStartOfDay());
                    if (shouldAlert) {
                        stock.setLastLowStockAlertAt(LocalDateTime.now());
                        stockRepository.save(stock);
                        emitAlertToOutbox("inventory-low-stock.v1", "LOW_STOCK", stock);
                    }
                }

                if (remainingToConsume == 0) {
                    break;
                }
            }
        }

        // If we still need to consume and couldn't find enough stock, take the first stock negative
        if (remainingToConsume > 0) {
            Stock firstLocation = stocks.get(0);
            log.warn("Low stock alert! Deficit of {} for item {}", remainingToConsume, itemId);
            firstLocation.setQuantity(firstLocation.getQuantity() - remainingToConsume);
            stockRepository.save(firstLocation);

            StockTransaction tx = new StockTransaction();
            tx.setItemId(itemId);
            tx.setQuantityChange(-remainingToConsume);
            tx.setType(TransactionType.CONSUMPTION);
            tx.setReason("Forced Patient Consumption deficit");
            tx.setReferenceId(referenceId);
            transactionRepository.save(tx);

            if (firstLocation.getQuantity() <= firstLocation.getMinThreshold()) {
                boolean shouldAlert = firstLocation.getLastLowStockAlertAt() == null || firstLocation.getLastLowStockAlertAt().isBefore(LocalDate.now().atStartOfDay());
                if (shouldAlert) {
                    firstLocation.setLastLowStockAlertAt(LocalDateTime.now());
                    stockRepository.save(firstLocation);
                    emitAlertToOutbox("inventory-low-stock.v1", "LOW_STOCK", firstLocation);
                }
            }
        }
    }

    @Transactional
    public Stock restock(UUID itemId, String location, Integer quantity, LocalDateTime expiryDate) {
        Stock stock = stockRepository.findByItemIdAndLocation(itemId, location).orElseGet(() -> {
            Stock s = new Stock();
            s.setItemId(itemId);
            s.setLocation(location);
            s.setQuantity(0);
            s.setMinThreshold(10); // default
            return s;
        });

        stock.setQuantity(stock.getQuantity() + quantity);
        if (expiryDate != null) {
            stock.setExpiryDate(expiryDate);
        }
        Stock savedStock = stockRepository.save(stock);

        StockTransaction tx = new StockTransaction();
        tx.setItemId(itemId);
        tx.setQuantityChange(quantity);
        tx.setType(TransactionType.RESTOCK);
        tx.setReason("Supply order restock to " + location);
        transactionRepository.save(tx);

        return savedStock;
    }

    @Transactional
    public void transfer(UUID itemId, String fromLocation, String toLocation, Integer quantity) {
        Stock sourceStock = stockRepository.findByItemIdAndLocation(itemId, fromLocation)
            .orElseThrow(() -> new RuntimeException("Source stock not found"));
        
        if (sourceStock.getQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock to transfer");
        }

        Stock destStock = stockRepository.findByItemIdAndLocation(itemId, toLocation).orElseGet(() -> {
            Stock s = new Stock();
            s.setItemId(itemId);
            s.setLocation(toLocation);
            s.setQuantity(0);
            s.setMinThreshold(10);
            return s;
        });

        sourceStock.setQuantity(sourceStock.getQuantity() - quantity);
        destStock.setQuantity(destStock.getQuantity() + quantity);

        stockRepository.save(sourceStock);
        stockRepository.save(destStock);

        StockTransaction txOut = new StockTransaction();
        txOut.setItemId(itemId);
        txOut.setQuantityChange(-quantity);
        txOut.setType(TransactionType.TRANSFER_OUT);
        txOut.setReason("Transfer to " + toLocation);
        transactionRepository.save(txOut);

        StockTransaction txIn = new StockTransaction();
        txIn.setItemId(itemId);
        txIn.setQuantityChange(quantity);
        txIn.setType(TransactionType.TRANSFER_IN);
        txIn.setReason("Transfer from " + fromLocation);
        transactionRepository.save(txIn);
    }

    @Transactional
    public int processExpirations() {
        List<Stock> expiredStocks = stockRepository.findByExpiryDateBefore(LocalDateTime.now());
        int count = 0;
        
        for (Stock stock : expiredStocks) {
            if (stock.getQuantity() > 0) {
                int loss = stock.getQuantity();
                stock.setQuantity(0);
                stockRepository.save(stock);
                
                StockTransaction tx = new StockTransaction();
                tx.setItemId(stock.getItemId());
                tx.setQuantityChange(-loss);
                tx.setType(TransactionType.EXPIRED);
                tx.setReason("Stock expired at " + stock.getLocation());
                transactionRepository.save(tx);
                
                boolean shouldAlert = stock.getLastExpiredAlertAt() == null || stock.getLastExpiredAlertAt().isBefore(LocalDate.now().atStartOfDay());
                if (shouldAlert) {
                    stock.setLastExpiredAlertAt(LocalDateTime.now());
                    stockRepository.save(stock);
                    emitAlertToOutbox("inventory-item-expired.v1", "EXPIRED", stock);
                }

                log.warn("Item {} expired at {} with loss {}", stock.getItemId(), stock.getLocation(), loss);
                count += loss;
            }
        }
        return count;
    }

    private void emitAlertToOutbox(String topic, String alertType, Stock stock) {
        try {
            InventoryAlertEvent eventPayload = new InventoryAlertEvent();
            eventPayload.eventId = UUID.randomUUID().toString();
            eventPayload.itemId = stock.getItemId();
            eventPayload.location = stock.getLocation();
            eventPayload.currentQuantity = stock.getQuantity();
            eventPayload.threshold = stock.getMinThreshold();
            eventPayload.alertType = alertType;

            InventoryOutboxEvent ob = new InventoryOutboxEvent();
            ob.setAggregateType("Stock");
            ob.setAggregateId(stock.getItemId().toString());
            ob.setEventType(topic);
            ob.setPayloadJson(objectMapper.writeValueAsString(eventPayload));
            ob.setStatus("PENDING");
            ob.setRetryCount(0);
            ob.setCreatedAt(Instant.now());
            
            outboxRepository.save(ob);
            log.info("Emitted {} to outbox for item={}", topic, stock.getItemId());
        } catch (Exception ex) {
            log.error("Failed to serialize or save outbox event for stock alert", ex);
        }
    }
}
