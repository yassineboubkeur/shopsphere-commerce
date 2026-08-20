package com.shopsphere.payment_service.controller;

import com.shopsphere.payment_service.dto.PaymentRequest;
import com.shopsphere.payment_service.dto.PaymentResponse;
import com.shopsphere.payment_service.entity.Payment;
import com.shopsphere.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPaymentRequest(request));
    }

    @PostMapping("/process/success")
    public ResponseEntity<PaymentResponse> processPaymentSuccess(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPaymentSuccess(request));
    }

    @PostMapping("/process/failed")
    public ResponseEntity<PaymentResponse> processPaymentFailed(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPaymentFailed(request));
    }

    @GetMapping("/result/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentResult(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentResult(paymentId));
    }

    @GetMapping("/result/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentResultByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentResultByOrderId(orderId));
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.createPayment(payment));
    }

    @PostMapping("/{paymentId}/process")
    public ResponseEntity<Payment> processPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.processPayment(paymentId));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }
}
