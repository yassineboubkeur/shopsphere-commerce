package com.shopsphere.payment_service.service;

import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.event.PaymentEvent;
import com.shopsphere.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailureHandler {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    private static final String ORDER_SERVICE_URL = "http://localhost:8083/api/orders";
    private static final int MAX_RETRY = 3;

    @EventListener
    public void handlePaymentFailed(PaymentEvent event) {
        if (!"FAILED".equals(event.getEventType())) {
            return;
        }

        Payment payment = event.getPayment();
        log.info("Handling payment failure for order: {}", payment.getOrderId());

        cancelOrder(payment.getOrderId());
        logFailure(payment);
    }

    @EventListener
    public void handlePaymentRefunded(PaymentEvent event) {
        if (!"REFUNDED".equals(event.getEventType())) {
            return;
        }

        Payment payment = event.getPayment();
        log.info("Processing refund for order: {}", payment.getOrderId());
        cancelOrder(payment.getOrderId());
    }

    private void cancelOrder(Long orderId) {
        try {
            restTemplate.postForObject(
                    ORDER_SERVICE_URL + "/" + orderId + "/status",
                    Map.of("status", "CANCELLED"),
                    Object.class);
            log.info("Order {} cancelled due to payment issue", orderId);
        } catch (Exception e) {
            log.error("Failed to cancel order {}: {}", orderId, e.getMessage());
        }
    }

    private void logFailure(Payment payment) {
        log.error("=== PAYMENT FAILURE REPORT ===");
        log.error("Payment ID: {}", payment.getId());
        log.error("Order ID: {}", payment.getOrderId());
        log.error("User ID: {}", payment.getUserId());
        log.error("Amount: {}", payment.getAmount());
        log.error("Method: {}", payment.getPaymentMethod());
        log.error("Reason: {}", payment.getFailureReason());
        log.error("Time: {}", payment.getUpdatedAt());
        log.error("=============================");
    }
}
