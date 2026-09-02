package com.shopsphere.inventory_service.controller;

import com.shopsphere.inventory_service.entity.Inventory;
import com.shopsphere.inventory_service.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .productId(7L)
                .productName("Denim Jacket")
                .quantity(30)
                .reservedQuantity(5)
                .build();
    }

    @Test
    @DisplayName("GET /api/inventory/product/{id} - returns inventory by product")
    void getStockByProductId_returnsInventory() {
        when(inventoryService.getStockByProductId(7L)).thenReturn(inventory);

        ResponseEntity<Inventory> response = inventoryController.getStockByProductId(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProductName()).isEqualTo("Denim Jacket");
        assertThat(response.getBody().getAvailableQuantity()).isEqualTo(25);
    }

    @Test
    @DisplayName("GET /api/inventory - returns all inventory")
    void getAllStock_returnsList() {
        when(inventoryService.getAllStock()).thenReturn(List.of(inventory));

        ResponseEntity<List<Inventory>> response = inventoryController.getAllStock();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/inventory - creates an inventory record")
    void createInventory_returnsInventory() {
        when(inventoryService.createInventory(inventory)).thenReturn(inventory);

        ResponseEntity<Inventory> response = inventoryController.createInventory(inventory);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("PUT /api/inventory/product/{id} - updates the quantity")
    void updateQuantity_returnsInventory() {
        when(inventoryService.updateQuantity(7L, 40)).thenReturn(inventory);

        ResponseEntity<Inventory> response = inventoryController.updateQuantity(7L, Map.of("quantity", 40));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(inventoryService).updateQuantity(7L, 40);
    }

    @Test
    @DisplayName("POST /api/inventory/product/{id}/reserve - reserves stock")
    void reserveStock_returnsInventory() {
        when(inventoryService.reserveStock(7L, 3)).thenReturn(inventory);

        ResponseEntity<Inventory> response = inventoryController.reserveStock(7L, Map.of("quantity", 3));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(inventoryService).reserveStock(7L, 3);
    }

    @Test
    @DisplayName("POST /api/inventory/product/{id}/release - releases stock")
    void releaseStock_returnsInventory() {
        when(inventoryService.releaseStock(7L, 2)).thenReturn(inventory);

        ResponseEntity<Inventory> response = inventoryController.releaseStock(7L, Map.of("quantity", 2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(inventoryService).releaseStock(7L, 2);
    }

    @Test
    @DisplayName("GET /api/inventory/product/{id}/available - reports availability")
    void isAvailable_returnsMap() {
        when(inventoryService.isAvailable(7L, 5)).thenReturn(true);
        when(inventoryService.getStockByProductId(7L)).thenReturn(inventory);

        ResponseEntity<Map<String, Object>> response = inventoryController.isAvailable(7L, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("isAvailable")).isEqualTo(true);
        assertThat(response.getBody().get("availableQuantity")).isEqualTo(25);
        assertThat(response.getBody().get("productId")).isEqualTo(7L);
    }
}
