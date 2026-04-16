package com.project.inventory_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.inventory_service.constants.Endpoints;
import com.project.inventory_service.dto.ItemConsumedEvent;
import com.project.inventory_service.dto.RestockRequest;
import com.project.inventory_service.dto.TransferRequest;
import com.project.inventory_service.model.Item;
import com.project.inventory_service.model.Stock;
import com.project.inventory_service.repository.ItemRepository;
import com.project.inventory_service.repository.StockRepository;
import com.project.inventory_service.service.InventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.INVENTORY_BASE)
public class InventoryController {
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    @Value("${kafka.topic.inventory-item-consumed}")
    private String itemConsumedTopic;

    public InventoryController(
            ItemRepository itemRepository, 
            StockRepository stockRepository, 
            KafkaTemplate<String, String> kafkaTemplate, 
            ObjectMapper objectMapper, 
            InventoryService inventoryService) {
        this.itemRepository = itemRepository;
        this.stockRepository = stockRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @GetMapping(Endpoints.ITEMS)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @PostMapping(Endpoints.ITEMS)
    @PreAuthorize("hasRole('ADMIN')")
    public Item createItem(@RequestBody Item item) {
        return itemRepository.save(item);
    }

    @GetMapping(Endpoints.STOCKS)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    @PostMapping("/stocks/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public Stock restock(@RequestBody RestockRequest request) {
        return inventoryService.restock(request.getItemId(), request.getLocation(), request.getQuantity(), request.getExpiryDate());
    }

    @PostMapping("/stocks/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public String transfer(@RequestBody TransferRequest request) {
        inventoryService.transfer(request.getItemId(), request.getFromLocation(), request.getToLocation(), request.getQuantity());
        return "Transfer successful";
    }

    @GetMapping("/stocks/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Stock> getExpiredStocks() {
        return stockRepository.findByExpiryDateBefore(LocalDateTime.now());
    }

    @PostMapping("/stocks/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public String processExpirations() {
        int count = inventoryService.processExpirations();
        return count + " expired items processed.";
    }

    @PostMapping(Endpoints.STOCKS_INITIALIZE)
    @PreAuthorize("hasRole('ADMIN')")
    public Stock initializeStock(@RequestParam UUID itemId, @RequestParam Integer initialQuantity) {
        Stock stock = new Stock();
        stock.setItemId(itemId);
        stock.setQuantity(initialQuantity);
        stock.setMinThreshold(10);
        stock.setLocation("Default Location");
        return stockRepository.save(stock);
    }

    @PostMapping(Endpoints.TEST_CONSUME)
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL_SERVICE')")
    public String triggerConsumptionEvent(@RequestBody ItemConsumedEvent event) throws JsonProcessingException {
        if (event.getEventId() == null) event.setEventId(UUID.randomUUID().toString());
        if (event.getOccurredAt() == null) event.setOccurredAt(LocalDateTime.now());
        
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(itemConsumedTopic, event.getItemId().toString(), payload);
        return "Event triggered: " + event.getEventId();
    }
}
