package com.shopsphere.notification_service.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String orderNumber;
    private BigDecimal amount;
    private String paymentMethod;
    private String failureReason;
    private LocalDateTime timestamp;
}