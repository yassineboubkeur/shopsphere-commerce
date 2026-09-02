package com.shopsphere.payment_service.controller;

import com.shopsphere.payment_service.dto.PaymentRequest;
import com.shopsphere.payment_service.dto.PaymentResponse;
import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.service.PaymentService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(com.shopsphere.payment_service.security.SecurityConfig.class)
class PaymentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentResponse paymentResponse;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentResponse = PaymentResponse.builder()
                .paymentId(1L)
                .orderId(10L)
                .status("COMPLETED")
                .transactionId("TXN-001")
                .amount(new BigDecimal("59.99"))
                .paymentMethod("CREDIT_CARD")
                .message("Payment successful")
                .processedAt(LocalDateTime.now())
                .build();

        payment = Payment.builder()
                .id(1L)
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("59.99"))
                .status(Payment.PaymentStatus.COMPLETED)
                .paymentMethod("CREDIT_CARD")
                .transactionId("TXN-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/payments/process - process payment request")
    void processPayment_returnsResponse() throws Exception {
        when(paymentService.processPaymentRequest(any(PaymentRequest.class))).thenReturn(paymentResponse);
        PaymentRequest request = PaymentRequest.builder()
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("59.99"))
                .paymentMethod("CREDIT_CARD")
                .build();

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value("TXN-001"));
    }

    @Test
    @DisplayName("POST /api/payments/process/success - process payment success callback")
    void processPaymentSuccess_returnsResponse() throws Exception {
        when(paymentService.processPaymentSuccess(any(PaymentRequest.class))).thenReturn(paymentResponse);
        PaymentRequest request = PaymentRequest.builder()
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("59.99"))
                .paymentMethod("CREDIT_CARD")
                .build();

        mockMvc.perform(post("/api/payments/process/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/payments/process/failed - process payment failure callback")
    void processPaymentFailed_returnsResponse() throws Exception {
        PaymentResponse failedResponse = PaymentResponse.builder()
                .paymentId(1L).orderId(10L).status("FAILED").message("Payment failed").build();
        when(paymentService.processPaymentFailed(any(PaymentRequest.class))).thenReturn(failedResponse);
        PaymentRequest request = PaymentRequest.builder()
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("59.99"))
                .paymentMethod("CREDIT_CARD")
                .build();

        mockMvc.perform(post("/api/payments/process/failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("GET /api/payments/result/{paymentId} - returns payment result")
    void getPaymentResult_returnsResponse() throws Exception {
        when(paymentService.getPaymentResult(1L)).thenReturn(paymentResponse);

        mockMvc.perform(get("/api/payments/result/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/payments/result/order/{orderId} - returns payment result by order")
    void getPaymentResultByOrderId_returnsResponse() throws Exception {
        when(paymentService.getPaymentResultByOrderId(10L)).thenReturn(paymentResponse);

        mockMvc.perform(get("/api/payments/result/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10));
    }

    @Test
    @DisplayName("POST /api/payments - create payment entity")
    void createPayment_returnsEntity() throws Exception {
        when(paymentService.createPayment(any(Payment.class))).thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-001"));
    }

    @Test
    @DisplayName("POST /api/payments/{id}/process - process payment by id")
    void processPaymentById_returnsEntity() throws Exception {
        when(paymentService.processPayment(1L)).thenReturn(payment);

        mockMvc.perform(post("/api/payments/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/payments/{id} - get payment by id")
    void getPaymentById_returnsEntity() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} - get payment by order id")
    void getPaymentByOrderId_returnsEntity() throws Exception {
        when(paymentService.getPaymentByOrderId(10L)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10));
    }

    @Test
    @DisplayName("POST /api/payments/{id}/refund - refund payment")
    void refundPayment_returnsEntity() throws Exception {
        Payment refunded = Payment.builder().id(1L).status(Payment.PaymentStatus.REFUNDED).build();
        when(paymentService.refundPayment(1L)).thenReturn(refunded);

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("GET /api/payments/user/{userId} - get payments by user")
    void getPaymentsByUserId_returnsList() throws Exception {
        when(paymentService.getPaymentsByUserId(4L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/user/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(4));
    }
}