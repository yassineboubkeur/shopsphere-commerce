package com.shopsphere.payment_service.service;

import com.shopsphere.payment_service.dto.PaymentRequest;
import com.shopsphere.payment_service.dto.PaymentResponse;
import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.event.PaymentEvent;
import com.shopsphere.payment_service.event.PaymentEventPublisher;
import com.shopsphere.payment_service.event.PaymentFailedEvent;
import com.shopsphere.payment_service.event.PaymentSuccessfulEvent;
import com.shopsphere.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentValidationService validationService;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentEventPublisher paymentEventPublisher;

    private static final String ORDER_SERVICE_URL = "http://localhost:8083/api/orders";

    public PaymentResponse processPaymentRequest(PaymentRequest request) {
        List<String> errors = validationService.validate(request);
        if (!errors.isEmpty()) {
            return PaymentResponse.builder()
                    .orderId(request.getOrderId())
                    .status("FAILED")
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .message("Validation failed: " + String.join(", ", errors))
                    .processedAt(LocalDateTime.now())
                    .build();
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        notifyOrderService(payment.getOrderId(), payment.getTransactionId());
        eventPublisher.publishEvent(new PaymentEvent(this, payment, "COMPLETED"));
        publishPaymentSuccessful(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status("SUCCESS")
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .message("Payment processed successfully")
                .processedAt(payment.getUpdatedAt())
                .build();
    }

    public PaymentResponse getPaymentResult(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        String status = payment.getStatus() == Payment.PaymentStatus.COMPLETED ? "SUCCESS" : payment.getStatus().name();
        String message = payment.getStatus() == Payment.PaymentStatus.COMPLETED ? "Payment completed successfully"
                : payment.getStatus() == Payment.PaymentStatus.FAILED ? "Payment failed: " + payment.getFailureReason()
                : payment.getStatus() == Payment.PaymentStatus.REFUNDED ? "Payment has been refunded"
                : "Payment is " + payment.getStatus().name().toLowerCase();

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status(status)
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .message(message)
                .processedAt(payment.getUpdatedAt())
                .build();
    }

    public PaymentResponse getPaymentResultByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

        String status = payment.getStatus() == Payment.PaymentStatus.COMPLETED ? "SUCCESS" : payment.getStatus().name();
        String message = payment.getStatus() == Payment.PaymentStatus.COMPLETED ? "Payment completed successfully"
                : payment.getStatus() == Payment.PaymentStatus.FAILED ? "Payment failed: " + payment.getFailureReason()
                : payment.getStatus() == Payment.PaymentStatus.REFUNDED ? "Payment has been refunded"
                : "Payment is " + payment.getStatus().name().toLowerCase();

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status(status)
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .message(message)
                .processedAt(payment.getUpdatedAt())
                .build();
    }

    public PaymentResponse processPaymentSuccess(PaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        publishPaymentSuccessful(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status("SUCCESS")
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .message("Payment completed successfully")
                .processedAt(payment.getUpdatedAt())
                .build();
    }

    public PaymentResponse processPaymentFailed(PaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.FAILED)
                .failureReason("Card declined by bank")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        notifyOrderServiceFailed(request.getOrderId());
        eventPublisher.publishEvent(new PaymentEvent(this, payment, "FAILED"));
        publishPaymentFailed(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status("FAILED")
                .transactionId(null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .message("Payment failed: " + payment.getFailureReason())
                .processedAt(payment.getUpdatedAt())
                .build();
    }

    public Payment createPayment(Payment payment) {
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment processPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Payment cannot be processed, current status: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        publishPaymentSuccessful(payment);
        return payment;
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only COMPLETED payments can be refunded");
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        eventPublisher.publishEvent(new PaymentEvent(this, payment, "REFUNDED"));
        return payment;
    }

    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    private void publishPaymentSuccessful(Payment payment) {
        paymentEventPublisher.publishPaymentSuccessful(PaymentSuccessfulEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .orderNumber(null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .build());
    }

    private void publishPaymentFailed(Payment payment) {
        paymentEventPublisher.publishPaymentFailed(PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .orderNumber(null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .failureReason(payment.getFailureReason())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private void notifyOrderService(Long orderId, String transactionId) {
        try {
            restTemplate.postForObject(
                    ORDER_SERVICE_URL + "/" + orderId + "/payment/complete",
                    Map.of("transactionId", transactionId),
                    Object.class);
            log.info("Order Service notified: payment completed for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to notify Order Service for order {}: {}", orderId, e.getMessage());
        }
    }

    private void notifyOrderServiceFailed(Long orderId) {
        try {
            restTemplate.postForObject(
                    ORDER_SERVICE_URL + "/" + orderId + "/status",
                    Map.of("status", "CANCELLED"),
                    Object.class);
            log.info("Order Service notified: order {} cancelled due to payment failure", orderId);
        } catch (Exception e) {
            log.error("Failed to notify Order Service for order {}: {}", orderId, e.getMessage());
        }
    }
}
