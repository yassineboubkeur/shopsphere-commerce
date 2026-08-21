package com.shopsphere.inventory_service.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdatedEvent {

    private Long productId;
    private String productName;
    private Integer previousQuantity;
    private Integer newQuantity;
    private Integer reservedQuantity;
}
