package com.project.support_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.support_service.command.InventoryCommandService;
import com.project.support_service.command.LabCommandService;
import com.project.support_service.dto.ItemConsumedEvent;
import com.project.support_service.dto.LabOrderPlacedEvent;
import com.project.support_service.model.outbox.SupportProcessedEvent;
import com.project.support_service.repository.SupportProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(SupportEventConsumer.class);
    
    private final LabCommandService labCommandService;
    private final InventoryCommandService inventoryCommandService;
    private final SupportProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    private static final String CONSUMER_GROUP = "support-service-group";

    public SupportEventConsumer(LabCommandService labCommandService,
                                InventoryCommandService inventoryCommandService,
                                SupportProcessedEventRepository processedEventRepository,
                                ObjectMapper objectMapper) {
        this.labCommandService = labCommandService;
        this.inventoryCommandService = inventoryCommandService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "lab-order-placed.v1", groupId = CONSUMER_GROUP)
    public void consumeLabOrder(String message) throws Exception {
        log.info("Received LabOrderPlacedEvent: {}", message);
        LabOrderPlacedEvent event = objectMapper.readValue(message, LabOrderPlacedEvent.class);
        
        if (isProcessed(event.eventId)) return;

        labCommandService.createOrderFromEvent(event);
        markProcessed(event.eventId);
        log.info("Processed lab order: {}", event.orderId);
    }

    @Transactional
    @KafkaListener(topics = "inventory-item-consumed.v1", groupId = CONSUMER_GROUP)
    public void consumeInventoryConsumption(String message) throws Exception {
        log.info("Received ItemConsumedEvent: {}", message);
        ItemConsumedEvent event = objectMapper.readValue(message, ItemConsumedEvent.class);
        
        if (isProcessed(event.eventId)) return;

        inventoryCommandService.consumeItem(event.getItemId(), event.getQuantity(), java.util.UUID.fromString(event.getEventId()));
        markProcessed(event.eventId);
        log.info("Processed inventory consumption for item: {}", event.getItemId());
    }

    private boolean isProcessed(String eventId) {
        if (processedEventRepository.findByEventIdAndConsumerName(eventId, CONSUMER_GROUP).isPresent()) {
            log.info("Event already processed: {}", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(String eventId) {
        processedEventRepository.save(new SupportProcessedEvent(eventId, CONSUMER_GROUP));
    }
}
