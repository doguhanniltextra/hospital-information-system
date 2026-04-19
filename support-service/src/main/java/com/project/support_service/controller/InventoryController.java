package com.project.support_service.controller;

import com.project.support_service.command.InventoryCommandService;
import com.project.support_service.model.inventory.Item;
import com.project.support_service.model.inventory.Stock;
import com.project.support_service.query.InventoryQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryCommandService inventoryCommandService;
    private final InventoryQueryService inventoryQueryService;

    public InventoryController(InventoryCommandService inventoryCommandService, 
                               InventoryQueryService inventoryQueryService) {
        this.inventoryCommandService = inventoryCommandService;
        this.inventoryQueryService = inventoryQueryService;
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<Item> getAllItems() {
        return inventoryQueryService.getAllItems();
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public List<Stock> getAllStocks() {
        return inventoryQueryService.getAllStocks();
    }

    @GetMapping("/stocks/{itemId}")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public List<Stock> getStocksByItem(@PathVariable UUID itemId) {
        return inventoryQueryService.getStocksByItem(itemId);
    }

    @PostMapping("/stocks/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public Stock restock(@RequestParam UUID itemId, @RequestParam String location, 
                        @RequestParam Integer quantity, @RequestParam(required = false) LocalDateTime expiryDate) {
        return inventoryCommandService.restock(itemId, location, quantity, expiryDate);
    }
}
