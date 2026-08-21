package com.shopsphere.inventory_service.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInsufficientEvent {

    private Long orderId;
    private Long productId;
    private String productName;
    private Integer requestedQuantity;
    private Integer availableQuantity;
}
