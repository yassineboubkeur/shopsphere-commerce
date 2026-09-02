package com.shopsphere.inventory_service.service;

import com.shopsphere.inventory_service.entity.Inventory;
import com.shopsphere.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

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
    @DisplayName("getStockByProductId - returns the inventory record")
    void getStockByProductId_found() {
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));

        Inventory result = inventoryService.getStockByProductId(7L);

        assertThat(result.getProductName()).isEqualTo("Denim Jacket");
        assertThat(result.getAvailableQuantity()).isEqualTo(25);
    }

    @Test
    @DisplayName("getStockByProductId - throws when not found")
    void getStockByProductId_notFound() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStockByProductId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found");
    }

    @Test
    @DisplayName("getAllStock - returns all inventory records")
    void getAllStock_success() {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        List<Inventory> records = inventoryService.getAllStock();

        assertThat(records).hasSize(1);
    }

    @Test
    @DisplayName("createInventory - initializes reserved quantity and timestamps")
    void createInventory_success() {
        Inventory input = Inventory.builder().productId(7L).productName("Denim Jacket").quantity(10).build();
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.createInventory(input);

        assertThat(result.getReservedQuantity()).isZero();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateQuantity - updates quantity and publishes stock event")
    void updateQuantity_success() {
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory updated = inventoryService.updateQuantity(7L, 40);

        assertThat(updated.getQuantity()).isEqualTo(40);
        verify(eventPublisher).publishStockUpdated(any());
    }

    @Test
    @DisplayName("updateQuantity - throws when product not found")
    void updateQuantity_notFound() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.updateQuantity(99L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found");
        verify(eventPublisher, never()).publishStockUpdated(any());
    }

    @Test
    @DisplayName("reserveStock - reserves quantity when stock is sufficient")
    void reserveStock_success() {
        inventory.setReservedQuantity(5);
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.reserveStock(7L, 3);

        assertThat(result.getReservedQuantity()).isEqualTo(8);
        assertThat(result.getAvailableQuantity()).isEqualTo(22);
        verify(eventPublisher).publishStockUpdated(any());
    }

    @Test
    @DisplayName("reserveStock - throws when stock is insufficient")
    void reserveStock_insufficient() {
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(7L, 100))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("releaseStock - releases reserved quantity")
    void releaseStock_success() {
        inventory.setReservedQuantity(10);
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.releaseStock(7L, 4);

        assertThat(result.getReservedQuantity()).isEqualTo(6);
        verify(eventPublisher).publishStockUpdated(any());
    }

    @Test
    @DisplayName("releaseStock - never goes below zero reserved quantity")
    void releaseStock_floorAtZero() {
        inventory.setReservedQuantity(2);
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory result = inventoryService.releaseStock(7L, 100);

        assertThat(result.getReservedQuantity()).isZero();
    }

    @Test
    @DisplayName("isAvailable - true when enough stock is available")
    void isAvailable_enough() {
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));

        assertThat(inventoryService.isAvailable(7L, 5)).isTrue();
        assertThat(inventoryService.isAvailable(7L, 26)).isFalse();
    }

    @Test
    @DisplayName("isAvailable - throws when product not found")
    void isAvailable_notFound() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.isAvailable(99L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found");
    }
}
