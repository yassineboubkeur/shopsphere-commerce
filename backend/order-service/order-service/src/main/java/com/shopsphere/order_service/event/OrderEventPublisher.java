package com.shopsphere.order_service.event;

import com.shopsphere.order_service.config.KafkaConfig;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing OrderCreated event: orderId={} | orderNumber={} | total={}",
                    event.getOrderId(), event.getOrderNumber(), event.getTotalAmount());
            kafkaTemplate.send(KafkaConfig.ORDER_CREATED_TOPIC, String.valueOf(event.getOrderId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish OrderCreated event: {}", e.getMessage(), e);
        }
    }

    public void publishOrderShipped(OrderShippedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing OrderShipped event: orderId={} | tracking={} | carrier={}",
                    event.getOrderId(), event.getTrackingNumber(), event.getCarrier());
            kafkaTemplate.send(KafkaConfig.ORDER_SHIPPED_TOPIC, String.valueOf(event.getOrderId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish OrderShipped event: {}", e.getMessage(), e);
        }
    }

    public void publishOrderDelivered(OrderDeliveredEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing OrderDelivered event: orderId={}", event.getOrderId());
            kafkaTemplate.send(KafkaConfig.ORDER_DELIVERED_TOPIC, String.valueOf(event.getOrderId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish OrderDelivered event: {}", e.getMessage(), e);
        }
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing OrderCancelled event: orderId={} | orderNumber={}",
                    event.getOrderId(), event.getOrderNumber());
            kafkaTemplate.send(KafkaConfig.ORDER_CANCELLED_TOPIC, String.valueOf(event.getOrderId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish OrderCancelled event: {}", e.getMessage(), e);
        }
    }
}