package com.shopsphere.notification_service.service;

import com.shopsphere.notification_service.entity.Notification;
import com.shopsphere.notification_service.event.OrderCreatedEvent;
import com.shopsphere.notification_service.event.OrderCancelledEvent;
import com.shopsphere.notification_service.event.OrderDeliveredEvent;
import com.shopsphere.notification_service.event.OrderShippedEvent;
import com.shopsphere.notification_service.event.PaymentFailedEvent;
import com.shopsphere.notification_service.event.PaymentSuccessfulEvent;
import com.shopsphere.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(Long userId, String type, String subject, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .subject(subject)
                .message(message)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={} | type={} | user={}", saved.getId(), type, userId);
        return saved;
    }

    public Notification sendNotification(Notification notification) {
        try {
            log.info("Sending email notification to user {}: {}", notification.getUserId(), notification.getSubject());
            log.info("Message: {}", notification.getMessage());
            notification.setStatus("SENT");
            notification = notificationRepository.save(notification);
            log.info("Notification sent successfully: id={}", notification.getId());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification = notificationRepository.save(notification);
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
        return notification;
    }

    public Notification sendOrderConfirmation(OrderCreatedEvent event) {
        String subject = "Order Confirmation - " + event.getOrderNumber();
        String body = buildOrderConfirmationBody(event);
        Notification notification = createNotification(event.getUserId(), "ORDER_CONFIRMATION", subject, body);
        return sendNotification(notification);
    }

    public Notification sendPaymentConfirmation(PaymentSuccessfulEvent event) {
        String subject = "Payment Received - " + event.getOrderNumber();
        String body = buildPaymentConfirmationBody(event);
        Notification notification = createNotification(event.getUserId(), "PAYMENT_CONFIRMATION", subject, body);
        return sendNotification(notification);
    }

    public Notification sendShippingNotification(OrderShippedEvent event) {
        String subject = "Order Shipped - " + event.getOrderNumber();
        String body = buildShippingNotificationBody(event);
        Notification notification = createNotification(event.getUserId(), "SHIPPING_NOTIFICATION", subject, body);
        return sendNotification(notification);
    }

    public Notification sendPaymentFailedNotification(PaymentFailedEvent event) {
        String subject = "Payment Failed - " + event.getOrderNumber();
        String body = buildPaymentFailedBody(event);
        Notification notification = createNotification(event.getUserId(), "PAYMENT_FAILED", subject, body);
        return sendNotification(notification);
    }

    public Notification sendOrderDeliveredNotification(OrderDeliveredEvent event) {
        String subject = "Order Delivered - " + event.getOrderNumber();
        String body = buildOrderDeliveredBody(event);
        Notification notification = createNotification(event.getUserId(), "ORDER_DELIVERED", subject, body);
        return sendNotification(notification);
    }

    public Notification sendOrderCancelledNotification(OrderCancelledEvent event) {
        String subject = "Order Cancelled - " + event.getOrderNumber();
        String body = buildOrderCancelledBody(event);
        Notification notification = createNotification(event.getUserId(), "ORDER_CANCELLED", subject, body);
        return sendNotification(notification);
    }

    private String buildOrderConfirmationBody(OrderCreatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thank you for your order!\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Total Amount: $").append(event.getTotalAmount()).append("\n\n");
        sb.append("Items:\n");
        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            sb.append("  - ").append(item.getProductName())
              .append(" x").append(item.getQuantity())
              .append(" @ $").append(item.getPrice()).append("\n");
        }
        return sb.toString();
    }

    private String buildPaymentConfirmationBody(PaymentSuccessfulEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your payment has been processed successfully!\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Amount: $").append(event.getAmount()).append("\n");
        sb.append("Payment Method: ").append(event.getPaymentMethod()).append("\n");
        sb.append("Transaction ID: ").append(event.getTransactionId()).append("\n");
        return sb.toString();
    }

    private String buildShippingNotificationBody(OrderShippedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your order has been shipped!\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Carrier: ").append(event.getCarrier()).append("\n");
        sb.append("Tracking Number: ").append(event.getTrackingNumber()).append("\n");
        sb.append("Estimated Delivery: ").append(event.getEstimatedDelivery()).append("\n");
        return sb.toString();
    }

    private String buildPaymentFailedBody(PaymentFailedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Unfortunately, your payment could not be processed.\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Amount: $").append(event.getAmount()).append("\n");
        sb.append("Payment Method: ").append(event.getPaymentMethod()).append("\n");
        sb.append("Reason: ").append(event.getFailureReason()).append("\n");
        return sb.toString();
    }

    private String buildOrderDeliveredBody(OrderDeliveredEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Great news! Your order has been delivered.\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Delivered At: ").append(event.getDeliveredAt()).append("\n");
        return sb.toString();
    }

    private String buildOrderCancelledBody(OrderCancelledEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your order has been cancelled.\n\n");
        sb.append("Order Number: ").append(event.getOrderNumber()).append("\n");
        sb.append("Cancelled At: ").append(event.getCancelledAt()).append("\n");
        return sb.toString();
    }
}
