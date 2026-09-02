package com.shopsphere.payment_service.integration;

import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.repository.PaymentRepository;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRepositoryIT extends PostgresTestContainer {

    @MockitoBean
    private com.shopsphere.payment_service.event.PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("save - persists payment in PostgreSQL")
    void save_persistsPayment() {
        Payment payment = paymentRepository.save(Payment.builder()
                .orderId(4L)
                .userId(4L)
                .amount(new BigDecimal("99.99"))
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("CREDIT_CARD")
                .build());

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("findByOrderId - returns payment for order")
    void findByOrderId_returnsPayment() {
        paymentRepository.save(Payment.builder()
                .orderId(7L)
                .userId(4L)
                .amount(new BigDecimal("50.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .paymentMethod("STRIPE")
                .transactionId("TXN-123")
                .build());

        Optional<Payment> found = paymentRepository.findByOrderId(7L);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(found.get().getTransactionId()).isEqualTo("TXN-123");
    }

    @Test
    @DisplayName("update - changes payment status in PostgreSQL")
    void update_changesPaymentStatus() {
        Payment payment = paymentRepository.save(Payment.builder()
                .orderId(8L)
                .userId(4L)
                .amount(new BigDecimal("75.00"))
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("PAYPAL")
                .build());

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-456");
        paymentRepository.save(payment);

        Payment found = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(found.getTransactionId()).isEqualTo("TXN-456");
    }
}
