package com.shopsphere.payment_service.service;

import com.shopsphere.payment_service.dto.PaymentRequest;
import com.shopsphere.payment_service.dto.PaymentResponse;
import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.event.PaymentEventPublisher;
import com.shopsphere.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentValidationService validationService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequest.builder()
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("119.98"))
                .paymentMethod("CREDIT_CARD")
                .cardNumber("4111111111111111")
                .cardHolder("Yassine Boubkeur")
                .expiryDate("12/28")
                .cvv("123")
                .build();

        payment = Payment.builder()
                .id(1L)
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("119.98"))
                .paymentMethod("CREDIT_CARD")
                .status(Payment.PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("processPaymentRequest - returns FAILED when validation fails")
    void processPaymentRequest_validationFails() {
        when(validationService.validate(validRequest)).thenReturn(List.of("Card number must be 16 digits"));

        PaymentResponse response = paymentService.processPaymentRequest(validRequest);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("Card number must be 16 digits");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("processPaymentRequest - processes a valid payment to COMPLETED")
    void processPaymentRequest_success() {
        when(validationService.validate(validRequest)).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.processPaymentRequest(validRequest);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getTransactionId()).startsWith("TXN-");
        assertThat(response.getAmount()).isEqualByComparingTo("119.98");
        verify(paymentEventPublisher).publishPaymentSuccessful(any());
    }

    @Test
    @DisplayName("getPaymentResult - maps COMPLETED payment to SUCCESS")
    void getPaymentResult_completed() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-ABC12345");
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentResult(1L);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("Payment completed successfully");
        assertThat(response.getTransactionId()).isEqualTo("TXN-ABC12345");
    }

    @Test
    @DisplayName("getPaymentResult - not found throws")
    void getPaymentResult_notFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentResult(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment not found");
    }

    @Test
    @DisplayName("getPaymentResultByOrderId - returns the payment of an order")
    void getPaymentResultByOrderId_success() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentResultByOrderId(10L);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("processPaymentSuccess - saves a COMPLETED payment and publishes")
    void processPaymentSuccess_success() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.processPaymentSuccess(validRequest);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getTransactionId()).startsWith("TXN-");
        verify(paymentEventPublisher).publishPaymentSuccessful(any());
    }

    @Test
    @DisplayName("processPaymentFailed - saves FAILED payment and notifies")
    void processPaymentFailed_success() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.processPaymentFailed(validRequest);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("Card declined");
        verify(paymentEventPublisher).publishPaymentFailed(any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("createPayment - persists a PENDING payment")
    void createPayment_success() {
        Payment saved = Payment.builder().id(5L).orderId(10L).build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        Payment result = paymentService.createPayment(payment);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("processPayment - processes a PENDING payment to COMPLETED")
    void processPayment_success() {
        Payment pending = Payment.builder()
                .id(1L).orderId(10L).userId(4L)
                .amount(new BigDecimal("119.98"))
                .status(Payment.PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(1L);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(result.getTransactionId()).startsWith("TXN-");
        verify(paymentEventPublisher).publishPaymentSuccessful(any());
    }

    @Test
    @DisplayName("processPayment - throws when payment is not PENDING")
    void processPayment_notPending() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment cannot be processed");
        verify(paymentEventPublisher, never()).publishPaymentSuccessful(any());
    }

    @Test
    @DisplayName("getPaymentById - returns the payment")
    void getPaymentById_success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThat(paymentService.getPaymentById(1L)).isSameAs(payment);
    }

    @Test
    @DisplayName("getPaymentByOrderId - returns the payment of the order")
    void getPaymentByOrderId_success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));

        assertThat(paymentService.getPaymentByOrderId(10L)).isSameAs(payment);
    }

    @Test
    @DisplayName("refundPayment - refunds a COMPLETED payment")
    void refundPayment_success() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment refunded = paymentService.refundPayment(1L);

        assertThat(refunded.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("refundPayment - throws when not COMPLETED")
    void refundPayment_notCompleted() {
        payment.setStatus(Payment.PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only COMPLETED payments can be refunded");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getPaymentsByUserId - returns the payments of a user")
    void getPaymentsByUserId_success() {
        when(paymentRepository.findByUserId(4L)).thenReturn(List.of(payment));

        assertThat(paymentService.getPaymentsByUserId(4L)).hasSize(1);
    }
}
