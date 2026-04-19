package com.project.support_service.query;

import com.project.support_service.model.inventory.Item;
import com.project.support_service.model.inventory.Stock;
import com.project.support_service.model.inventory.StockTransaction;
import com.project.support_service.repository.ItemRepository;
import com.project.support_service.repository.StockRepository;
import com.project.support_service.repository.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryQueryService {
    private final ItemRepository itemRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;

    public InventoryQueryService(ItemRepository itemRepository,
                                 StockRepository stockRepository,
                                 TransactionRepository transactionRepository) {
        this.itemRepository = itemRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @Cacheable(value = "inventoryStocks", key = "#itemId")
    public List<Stock> getStocksByItem(UUID itemId) {
        return stockRepository.findByItemId(itemId);
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public List<StockTransaction> getTransactionsByItem(UUID itemId) {
        return transactionRepository.findByItemId(itemId);
    }
}
