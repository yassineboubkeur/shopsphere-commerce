package com.shopsphere.inventory_service.service;

import com.shopsphere.inventory_service.config.KafkaConfig;
import com.shopsphere.inventory_service.event.StockInsufficientEvent;
import com.shopsphere.inventory_service.event.StockUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishStockUpdated(StockUpdatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing StockUpdated event: productId={} | {} → {} | reserved={}",
                    event.getProductId(), event.getPreviousQuantity(), event.getNewQuantity(), event.getReservedQuantity());
            kafkaTemplate.send(KafkaConfig.STOCK_UPDATED_TOPIC, String.valueOf(event.getProductId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish StockUpdated event: {}", e.getMessage(), e);
        }
    }

    public void publishStockInsufficient(StockInsufficientEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing StockInsufficient event: orderId={} | productId={} | requested={} | available={}",
                    event.getOrderId(), event.getProductId(), event.getRequestedQuantity(), event.getAvailableQuantity());
            kafkaTemplate.send(KafkaConfig.STOCK_INSUFFICIENT_TOPIC, String.valueOf(event.getProductId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish StockInsufficient event: {}", e.getMessage(), e);
        }
    }
}
