package com.shopsphere.inventory_service.controller;

import com.shopsphere.inventory_service.entity.Inventory;
import com.shopsphere.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<Inventory> getStockByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStockByProductId(productId));
    }

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllStock() {
        return ResponseEntity.ok(inventoryService.getAllStock());
    }

    @PostMapping
    public ResponseEntity<Inventory> createInventory(@RequestBody Inventory inventory) {
        return ResponseEntity.ok(inventoryService.createInventory(inventory));
    }

    @PutMapping("/product/{productId}")
    public ResponseEntity<Inventory> updateQuantity(
            @PathVariable Long productId,
            @RequestBody java.util.Map<String, Integer> body) {
        return ResponseEntity.ok(inventoryService.updateQuantity(productId, body.get("quantity")));
    }

    @PostMapping("/product/{productId}/reserve")
    public ResponseEntity<Inventory> reserveStock(
            @PathVariable Long productId,
            @RequestBody java.util.Map<String, Integer> body) {
        return ResponseEntity.ok(inventoryService.reserveStock(productId, body.get("quantity")));
    }

    @PostMapping("/product/{productId}/release")
    public ResponseEntity<Inventory> releaseStock(
            @PathVariable Long productId,
            @RequestBody java.util.Map<String, Integer> body) {
        return ResponseEntity.ok(inventoryService.releaseStock(productId, body.get("quantity")));
    }

    @GetMapping("/product/{productId}/available")
    public ResponseEntity<java.util.Map<String, Object>> isAvailable(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        boolean available = inventoryService.isAvailable(productId, quantity);
        Inventory inventory = inventoryService.getStockByProductId(productId);
        return ResponseEntity.ok(java.util.Map.of(
                "productId", productId,
                "requestedQuantity", quantity,
                "availableQuantity", inventory.getAvailableQuantity(),
                "isAvailable", available));
    }
}
