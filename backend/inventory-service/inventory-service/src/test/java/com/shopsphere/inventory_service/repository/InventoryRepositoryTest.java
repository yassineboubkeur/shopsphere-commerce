package com.shopsphere.inventory_service.repository;

import com.shopsphere.inventory_service.entity.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("save - persists an inventory record")
    void save() {
        Inventory inventory = inventoryRepository.save(buildInventory(7L));

        assertThat(inventory.getId()).isNotNull();
        assertThat(inventory.getQuantity()).isEqualTo(30);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("findByProductId - returns the inventory record")
    void findByProductId() {
        inventoryRepository.save(buildInventory(7L));

        Optional<Inventory> found = inventoryRepository.findByProductId(7L);
        Optional<Inventory> missing = inventoryRepository.findByProductId(999L);

        assertThat(found).isPresent();
        assertThat(found.get().getAvailableQuantity()).isEqualTo(25);
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findAll - returns all inventory records")
    void findAll() {
        inventoryRepository.save(buildInventory(7L));
        Inventory second = buildInventory(9L);
        second.setProductName("Sneakers");
        inventoryRepository.save(second);

        List<Inventory> records = inventoryRepository.findAll();

        assertThat(records).hasSize(2);
        assertThat(records).extracting(Inventory::getProductName)
                .containsExactlyInAnyOrder("Denim Jacket", "Sneakers");
    }

    @Test
    @DisplayName("getAvailableQuantity - is quantity minus reserved")
    void availableQuantity() {
        Inventory inventory = Inventory.builder()
                .productId(7L)
                .productName("Denim Jacket")
                .quantity(10)
                .reservedQuantity(4)
                .build();
        inventoryRepository.save(inventory);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(6);
    }

    private Inventory buildInventory(Long productId) {
        return Inventory.builder()
                .productId(productId)
                .productName("Denim Jacket")
                .quantity(30)
                .reservedQuantity(5)
                .build();
    }
}