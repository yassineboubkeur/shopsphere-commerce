package com.shopsphere.notification_service.consumer;

import com.shopsphere.notification_service.config.KafkaConfig;
import com.shopsphere.notification_service.event.OrderCreatedEvent;
import com.shopsphere.notification_service.event.OrderDeliveredEvent;
import com.shopsphere.notification_service.event.OrderShippedEvent;
import com.shopsphere.notification_service.event.PaymentFailedEvent;
import com.shopsphere.notification_service.event.PaymentSuccessfulEvent;
import com.shopsphere.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC, groupId = "notification-service")
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("=== NOTIFICATION: Order Created ===");
            notificationService.sendOrderConfirmation(event);
        } catch (Exception e) {
            log.error("Failed to process OrderCreated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC, groupId = "notification-service")
    public void handlePaymentSuccessful(String message) {
        try {
            PaymentSuccessfulEvent event = objectMapper.readValue(message, PaymentSuccessfulEvent.class);
            log.info("=== NOTIFICATION: Payment Successful ===");
            notificationService.sendPaymentConfirmation(event);
        } catch (Exception e) {
            log.error("Failed to process PaymentSuccessful event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.ORDER_SHIPPED_TOPIC, groupId = "notification-service")
    public void handleOrderShipped(String message) {
        try {
            OrderShippedEvent event = objectMapper.readValue(message, OrderShippedEvent.class);
            log.info("=== NOTIFICATION: Order Shipped ===");
            notificationService.sendShippingNotification(event);
        } catch (Exception e) {
            log.error("Failed to process OrderShipped event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC, groupId = "notification-service")
    public void handlePaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            log.info("=== NOTIFICATION: Payment Failed ===");
            notificationService.sendPaymentFailedNotification(event);
        } catch (Exception e) {
            log.error("Failed to process PaymentFailed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.ORDER_DELIVERED_TOPIC, groupId = "notification-service")
    public void handleOrderDelivered(String message) {
        try {
            OrderDeliveredEvent event = objectMapper.readValue(message, OrderDeliveredEvent.class);
            log.info("=== NOTIFICATION: Order Delivered ===");
            notificationService.sendOrderDeliveredNotification(event);
        } catch (Exception e) {
            log.error("Failed to process OrderDelivered event: {}", e.getMessage(), e);
        }
    }
}
