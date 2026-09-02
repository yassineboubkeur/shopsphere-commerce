package com.shopsphere.payment_service.repository;

import com.shopsphere.payment_service.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("save - persists a payment")
    void save() {
        Payment payment = paymentRepository.save(buildPayment());

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("findByOrderId - returns the payment of an order")
    void findByOrderId() {
        paymentRepository.save(buildPayment());

        Optional<Payment> found = paymentRepository.findByOrderId(10L);
        Optional<Payment> missing = paymentRepository.findByOrderId(999L);

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("119.98");
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findByUserId - returns all payments of a user")
    void findByUserId() {
        paymentRepository.save(buildPayment());
        paymentRepository.save(Payment.builder()
                .orderId(11L)
                .userId(4L)
                .amount(new BigDecimal("25.00"))
                .paymentMethod("PAYPAL")
                .status(Payment.PaymentStatus.PENDING)
                .build());
        paymentRepository.save(Payment.builder()
                .orderId(12L)
                .userId(5L)
                .amount(new BigDecimal("10.00"))
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.PENDING)
                .build());

        List<Payment> payments = paymentRepository.findByUserId(4L);

        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getOrderId).contains(10L, 11L);
    }

    @Test
    @DisplayName("findByTransactionId - returns the payment")
    void findByTransactionId() {
        paymentRepository.save(buildPayment());

        Optional<Payment> found = paymentRepository.findByTransactionId("TXN-ABC12345");
        Optional<Payment> missing = paymentRepository.findByTransactionId("NOPE");

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(10L);
        assertThat(missing).isEmpty();
    }

    private Payment buildPayment() {
        return Payment.builder()
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("119.98"))
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("TXN-ABC12345")
                .build();
    }
}