package com.project.support_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.support_service.model.inventory.Stock;
import com.project.support_service.repository.StockRepository;
import com.project.support_service.repository.SupportOutboxRepository;
import com.project.support_service.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryCommandServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SupportOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InventoryCommandService inventoryCommandService;

    @Test
    public void consumeItem_ShouldDeductFromMultipleStocks() {
        UUID itemId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        
        Stock stock1 = new Stock();
        stock1.setItemId(itemId);
        stock1.setQuantity(10);
        stock1.setMinThreshold(5);
        
        Stock stock2 = new Stock();
        stock2.setItemId(itemId);
        stock2.setQuantity(10);
        stock2.setMinThreshold(5);

        when(stockRepository.findByItemId(itemId)).thenReturn(Arrays.asList(stock1, stock2));

        inventoryCommandService.consumeItem(itemId, 15, referenceId);

        assertThat(stock1.getQuantity()).isEqualTo(0);
        assertThat(stock2.getQuantity()).isEqualTo(5);
        
        verify(stockRepository, times(2)).save(any());
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    public void consumeItem_WithDeficit_ShouldGoNegative() {
        UUID itemId = UUID.randomUUID();
        Stock stock = new Stock();
        stock.setItemId(itemId);
        stock.setQuantity(5);
        stock.setMinThreshold(0);

        when(stockRepository.findByItemId(itemId)).thenReturn(Collections.singletonList(stock));

        inventoryCommandService.consumeItem(itemId, 10, UUID.randomUUID());

        assertThat(stock.getQuantity()).isEqualTo(-5);
        verify(stockRepository, times(2)).save(stock);
    }

    @Test
    public void restock_ShouldUpdateExistingStock() {
        UUID itemId = UUID.randomUUID();
        String location = "Warehouse A";
        Stock existingStock = new Stock();
        existingStock.setItemId(itemId);
        existingStock.setQuantity(20);

        when(stockRepository.findByItemIdAndLocation(itemId, location)).thenReturn(Optional.of(existingStock));
        when(stockRepository.save(any())).thenReturn(existingStock);

        inventoryCommandService.restock(itemId, location, 10, null);

        assertThat(existingStock.getQuantity()).isEqualTo(30);
        verify(transactionRepository).save(any());
    }

    @Test
    public void checkThresholdAndAlert_ShouldEmitOutboxEvent() throws JsonProcessingException {
        UUID itemId = UUID.randomUUID();
        Stock stock = new Stock();
        stock.setItemId(itemId);
        stock.setQuantity(10);
        stock.setMinThreshold(10);
        stock.setLocation("Lab 1");

        when(stockRepository.findByItemId(itemId)).thenReturn(Collections.singletonList(stock));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        inventoryCommandService.consumeItem(itemId, 1, UUID.randomUUID());

        verify(outboxRepository).save(any());
    }
}
