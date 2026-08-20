package com.shopsphere.order_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private Long orderId;
    private String orderNumber;
    private String orderStatus;
    private BigDecimal totalAmount;
    private PaymentInfo payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private Long id;
        private String status;
        private String paymentMethod;
        private BigDecimal amount;
    }
}
