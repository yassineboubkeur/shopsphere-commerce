package com.shopsphere.order_service.event;

import lombok.*;

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