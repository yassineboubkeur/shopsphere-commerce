package com.shopsphere.inventory_service.service;

import com.shopsphere.inventory_service.config.KafkaConfig;
import com.shopsphere.inventory_service.event.OrderCreatedEvent;
import com.shopsphere.inventory_service.event.StockInsufficientEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC, groupId = "inventory-service-v2")
    public void handleOrderCreated(String message) {
        try {
            log.info("=== ORDER CREATED EVENT RECEIVED ===");

            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("Order ID: {} | User: {} | Total: {}", event.getOrderId(), event.getUserId(), event.getTotalAmount());

            for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
                try {
                    log.info("Reserving stock for product {} (ID: {}) | Qty: {}",
                            item.getProductName(), item.getProductId(), item.getQuantity());
                    inventoryService.reserveStock(item.getProductId(), item.getQuantity());
                    log.info("Stock reserved for product {}", item.getProductId());
                } catch (RuntimeException e) {
                    log.warn("Insufficient stock for product {}: {}", item.getProductId(), e.getMessage());
                    StockInsufficientEvent insufficientEvent = StockInsufficientEvent.builder()
                            .orderId(event.getOrderId())
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .requestedQuantity(item.getQuantity())
                            .availableQuantity(inventoryService.getStockByProductId(item.getProductId()).getAvailableQuantity())
                            .build();
                    eventPublisher.publishStockInsufficient(insufficientEvent);
                }
            }

            log.info("=== ORDER {} PROCESSED ===", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderCreated event: {}", e.getMessage(), e);
        }
    }
}
