package com.shopsphere.payment_service.controller;

import com.shopsphere.payment_service.dto.PaymentRequest;
import com.shopsphere.payment_service.dto.PaymentResponse;
import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentRequest request;
    private Payment payment;

    @BeforeEach
    void setUp() {
        request = PaymentRequest.builder()
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
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("TXN-ABC12345")
                .build();
    }

    @Test
    @DisplayName("POST /api/payments/process - processes the payment")
    void processPayment_returnsResponse() {
        PaymentResponse response = PaymentResponse.builder()
                .status("SUCCESS").orderId(10L).transactionId("TXN-ABC12345").build();
        when(paymentService.processPaymentRequest(request)).thenReturn(response);

        ResponseEntity<PaymentResponse> result = paymentController.processPayment(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST /api/payments/process/success - processes success")
    void processPaymentSuccess_returnsResponse() {
        PaymentResponse response = PaymentResponse.builder().status("SUCCESS").build();
        when(paymentService.processPaymentSuccess(request)).thenReturn(response);

        ResponseEntity<PaymentResponse> result = paymentController.processPaymentSuccess(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST /api/payments/process/failed - processes failure")
    void processPaymentFailed_returnsResponse() {
        PaymentResponse response = PaymentResponse.builder().status("FAILED").build();
        when(paymentService.processPaymentFailed(request)).thenReturn(response);

        ResponseEntity<PaymentResponse> result = paymentController.processPaymentFailed(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("GET /api/payments/result/{id} - returns the result")
    void getPaymentResult_returnsResponse() {
        PaymentResponse response = PaymentResponse.builder().status("SUCCESS").build();
        when(paymentService.getPaymentResult(1L)).thenReturn(response);

        ResponseEntity<PaymentResponse> result = paymentController.getPaymentResult(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("GET /api/payments/result/order/{id} - returns the result by order")
    void getPaymentResultByOrderId_returnsResponse() {
        PaymentResponse response = PaymentResponse.builder().status("SUCCESS").build();
        when(paymentService.getPaymentResultByOrderId(10L)).thenReturn(response);

        ResponseEntity<PaymentResponse> result = paymentController.getPaymentResultByOrderId(10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST /api/payments - creates a payment")
    void createPayment_returnsPayment() {
        when(paymentService.createPayment(payment)).thenReturn(payment);

        ResponseEntity<Payment> result = paymentController.createPayment(payment);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("POST /api/payments/{id}/process - processes payment by id")
    void processPaymentById_returnsPayment() {
        when(paymentService.processPayment(1L)).thenReturn(payment);

        ResponseEntity<Payment> result = paymentController.processPaymentById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("GET /api/payments/{id} - returns payment by id")
    void getPaymentById_returnsPayment() {
        when(paymentService.getPaymentById(1L)).thenReturn(payment);

        ResponseEntity<Payment> result = paymentController.getPaymentById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("GET /api/payments/order/{id} - returns payment by order")
    void getPaymentByOrderId_returnsPayment() {
        when(paymentService.getPaymentByOrderId(10L)).thenReturn(payment);

        ResponseEntity<Payment> result = paymentController.getPaymentByOrderId(10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("POST /api/payments/{id}/refund - refunds the payment")
    void refundPayment_returnsPayment() {
        when(paymentService.refundPayment(1L)).thenReturn(payment);

        ResponseEntity<Payment> result = paymentController.refundPayment(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).refundPayment(1L);
    }

    @Test
    @DisplayName("GET /api/payments/user/{id} - returns user payments")
    void getPaymentsByUserId_returnsPayments() {
        when(paymentService.getPaymentsByUserId(4L)).thenReturn(List.of(payment));

        ResponseEntity<List<Payment>> result = paymentController.getPaymentsByUserId(4L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }
}
