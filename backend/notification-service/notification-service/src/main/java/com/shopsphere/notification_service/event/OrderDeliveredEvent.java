package com.shopsphere.notification_service.event;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent {

    private Long orderId;
    private Long userId;
    private String orderNumber;
    private LocalDateTime deliveredAt;
}