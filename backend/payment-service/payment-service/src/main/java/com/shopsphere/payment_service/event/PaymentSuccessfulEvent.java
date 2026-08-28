package com.shopsphere.payment_service.event;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessfulEvent {

    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String orderNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
}