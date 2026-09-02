package com.shopsphere.inventory_service.controller;

import com.shopsphere.inventory_service.entity.Inventory;
import com.shopsphere.inventory_service.service.InventoryService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import(com.shopsphere.inventory_service.security.SecurityConfig.class)
class InventoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(1L)
                .productId(7L)
                .productName("Denim Jacket")
                .quantity(30)
                .reservedQuantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/inventory/product/{id} - returns stock for product")
    void getStockByProductId_returnsInventory() throws Exception {
        when(inventoryService.getStockByProductId(7L)).thenReturn(inventory);

        mockMvc.perform(get("/api/inventory/product/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Denim Jacket"))
                .andExpect(jsonPath("$.quantity").value(30))
                .andExpect(jsonPath("$.reservedQuantity").value(2));
    }

    @Test
    @DisplayName("GET /api/inventory - returns all stock entries")
    void getAllStock_returnsList() throws Exception {
        when(inventoryService.getAllStock()).thenReturn(List.of(inventory));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Denim Jacket"));
    }

    @Test
    @DisplayName("POST /api/inventory - create inventory entry")
    void createInventory_returns200() throws Exception {
        when(inventoryService.createInventory(any(Inventory.class))).thenReturn(inventory);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inventory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Denim Jacket"));
    }

    @Test
    @DisplayName("PUT /api/inventory/product/{id} - update quantity")
    void updateQuantity_returns200() throws Exception {
        Inventory updated = Inventory.builder()
                .id(1L).productId(7L).productName("Denim Jacket")
                .quantity(50).reservedQuantity(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(inventoryService.updateQuantity(7L, 50)).thenReturn(updated);

        mockMvc.perform(put("/api/inventory/product/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    @DisplayName("POST /api/inventory/product/{id}/reserve - reserve stock")
    void reserveStock_returns200() throws Exception {
        when(inventoryService.reserveStock(7L, 3)).thenReturn(inventory);

        mockMvc.perform(post("/api/inventory/product/7/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Denim Jacket"));
    }

    @Test
    @DisplayName("POST /api/inventory/product/{id}/release - release reserved stock")
    void releaseStock_returns200() throws Exception {
        when(inventoryService.releaseStock(7L, 1)).thenReturn(inventory);

        mockMvc.perform(post("/api/inventory/product/7/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/inventory/product/{id}/available - check availability")
    void isAvailable_returnsAvailabilityInfo() throws Exception {
        when(inventoryService.isAvailable(7L, 5)).thenReturn(true);
        when(inventoryService.getStockByProductId(7L)).thenReturn(inventory);

        mockMvc.perform(get("/api/inventory/product/7/available")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(7))
                .andExpect(jsonPath("$.requestedQuantity").value(5))
                .andExpect(jsonPath("$.availableQuantity").value(28))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    @DisplayName("GET /api/inventory/product/{id}/available - reports insufficient stock")
    void isAvailable_reportsInsufficient() throws Exception {
        when(inventoryService.isAvailable(7L, 100)).thenReturn(false);
        when(inventoryService.getStockByProductId(7L)).thenReturn(inventory);

        mockMvc.perform(get("/api/inventory/product/7/available")
                        .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAvailable").value(false));
    }
}