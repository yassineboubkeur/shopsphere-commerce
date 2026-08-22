package com.shopsphere.notification_service.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippedEvent {

    private Long orderId;
    private Long userId;
    private String orderNumber;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime estimatedDelivery;
}
