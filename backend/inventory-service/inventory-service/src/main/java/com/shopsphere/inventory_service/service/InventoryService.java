package com.shopsphere.inventory_service.service;

import com.shopsphere.inventory_service.entity.Inventory;
import com.shopsphere.inventory_service.event.StockUpdatedEvent;
import com.shopsphere.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventPublisher eventPublisher;

    public Inventory getStockByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
    }

    public List<Inventory> getAllStock() {
        return inventoryRepository.findAll();
    }

    public Inventory createInventory(Inventory inventory) {
        inventory.setReservedQuantity(0);
        inventory.setCreatedAt(LocalDateTime.now());
        inventory.setUpdatedAt(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }

    public Inventory updateQuantity(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
        Integer previousQuantity = inventory.getQuantity();
        inventory.setQuantity(quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        publishStockUpdated(saved, previousQuantity);
        return saved;
    }

    public Inventory reserveStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
        if (inventory.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product: " + productId
                    + " | Available: " + inventory.getAvailableQuantity()
                    + " | Requested: " + quantity);
        }
        Integer previousQuantity = inventory.getQuantity();
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        publishStockUpdated(saved, previousQuantity);
        return saved;
    }

    public Inventory releaseStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
        Integer previousQuantity = inventory.getQuantity();
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        publishStockUpdated(saved, previousQuantity);
        return saved;
    }

    public boolean isAvailable(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
        return inventory.getAvailableQuantity() >= quantity;
    }

    private void publishStockUpdated(Inventory inventory, Integer previousQuantity) {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .previousQuantity(previousQuantity)
                .newQuantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .build();
        eventPublisher.publishStockUpdated(event);
    }
}
