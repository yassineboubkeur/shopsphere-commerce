package com.shopsphere.inventory_service.integration;

import com.shopsphere.inventory_service.entity.Inventory;
import com.shopsphere.inventory_service.repository.InventoryRepository;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryRepositoryIT extends PostgresTestContainer {

    @MockitoBean
    private com.shopsphere.inventory_service.service.InventoryEventPublisher inventoryEventPublisher;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("save - persists inventory record in PostgreSQL")
    void save_persistsInventoryRecord() {
        Inventory inventory = inventoryRepository.save(Inventory.builder()
                .productId(1L)
                .productName("Denim Jacket")
                .quantity(50)
                .reservedQuantity(0)
                .build());

        assertThat(inventory.getId()).isNotNull();
        assertThat(inventory.getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("findByProductId - returns inventory for product")
    void findByProductId_returnsInventory() {
        inventoryRepository.save(Inventory.builder()
                .productId(1L)
                .productName("Denim Jacket")
                .quantity(50)
                .reservedQuantity(0)
                .build());

        Optional<Inventory> found = inventoryRepository.findByProductId(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getProductName()).isEqualTo("Denim Jacket");
        assertThat(found.get().getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("update stock - changes quantity in PostgreSQL")
    void updateStock_changesQuantity() {
        Inventory inventory = inventoryRepository.save(Inventory.builder()
                .productId(1L)
                .productName("Denim Jacket")
                .quantity(50)
                .reservedQuantity(0)
                .build());

        inventory.setQuantity(45);
        inventoryRepository.save(inventory);

        Inventory found = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(found.getQuantity()).isEqualTo(45);
    }
}
