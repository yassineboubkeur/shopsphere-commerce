package com.shopsphere.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String status;
    private String transactionId;
    private BigDecimal amount;
    private String paymentMethod;
    private String message;
    private LocalDateTime processedAt;
}
