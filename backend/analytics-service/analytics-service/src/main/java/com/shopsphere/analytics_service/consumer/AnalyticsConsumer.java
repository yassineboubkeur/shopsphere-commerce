package com.shopsphere.analytics_service.consumer;

import com.shopsphere.analytics_service.config.KafkaConfig;
import com.shopsphere.analytics_service.entity.AnalyticsEvent;
import com.shopsphere.analytics_service.event.OrderCreatedEvent;
import com.shopsphere.analytics_service.event.PaymentSuccessfulEvent;
import com.shopsphere.analytics_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final ObjectMapper objectMapper;
    private final AnalyticsService analyticsService;

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC, groupId = "analytics-service")
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("=== ANALYTICS: Order Created === Order: {} | Items: {} | Total: ${}",
                    event.getOrderNumber(), event.getItems().size(), event.getTotalAmount());

            List<AnalyticsEvent> events = event.getItems().stream()
                    .map(item -> AnalyticsEvent.builder()
                            .eventType("ORDER_CREATED")
                            .orderId(event.getOrderId())
                            .userId(event.getUserId())
                            .orderNumber(event.getOrderNumber())
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .amount(item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                            .status("CREATED")
                            .eventTimestamp(LocalDateTime.now())
                            .build())
                    .toList();

            analyticsService.saveAll(events);
            log.info("Saved {} analytics events for order {}", events.size(), event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to process OrderCreated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC, groupId = "analytics-service")
    public void handlePaymentSuccessful(String message) {
        try {
            PaymentSuccessfulEvent event = objectMapper.readValue(message, PaymentSuccessfulEvent.class);
            log.info("=== ANALYTICS: Payment Successful === Order: {} | Amount: ${} | Method: {}",
                    event.getOrderNumber(), event.getAmount(), event.getPaymentMethod());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .eventType("PAYMENT_SUCCESSFUL")
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .orderNumber(event.getOrderNumber())
                    .amount(event.getAmount())
                    .paymentMethod(event.getPaymentMethod())
                    .status("PAID")
                    .eventTimestamp(LocalDateTime.now())
                    .build();

            analyticsService.save(analyticsEvent);
            log.info("Saved payment analytics event for order {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to process PaymentSuccessful event: {}", e.getMessage(), e);
        }
    }
}
