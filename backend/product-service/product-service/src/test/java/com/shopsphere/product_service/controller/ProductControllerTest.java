package com.shopsphere.product_service.controller;

import com.shopsphere.product_service.dto.ProductRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.entity.Product;
import com.shopsphere.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(2L).name("Electronics").build();
        product = Product.builder()
                .id(1L)
                .name("Gaming Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("GET / - returns all products")
    void findAll_success() {
        when(productService.findAll()).thenReturn(List.of(product));

        ResponseEntity<List<Product>> response = productController.findAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("GET /{id} - returns a single product")
    void findById_success() {
        when(productService.findById(1L)).thenReturn(product);

        ResponseEntity<Product> response = productController.findById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    @DisplayName("POST / - returns 201 Created for an admin")
    void create_asAdmin() {
        when(productService.create(any(ProductRequest.class))).thenReturn(product);

        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .categoryId(2L)
                .build();

        ResponseEntity<Product> response = productController.create("ROLE_ADMIN", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(product);
        verify(productService).create(any(ProductRequest.class));
    }

    @Test
    @DisplayName("POST / - returns 403 for a non-admin role")
    void create_asUser_forbidden() {
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .categoryId(2L)
                .build();

        ResponseEntity<Product> response = productController.create("USER", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(productService, never()).create(any());
    }

    @Test
    @DisplayName("PATCH /{id}/stock - admin updates stock")
    void updateStock_asAdmin() {
        when(productService.updateStock(1L, 20)).thenReturn(product);

        ResponseEntity<Product> response = productController.updateStock(1L, "ROLE_ADMIN", Map.of("quantity", 20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).updateStock(1L, 20);
    }

    @Test
    @DisplayName("PATCH /{id}/stock - returns 400 when quantity is negative")
    void updateStock_negativeQuantity() {
        ResponseEntity<Product> response = productController.updateStock(1L, "ROLE_ADMIN", Map.of("quantity", -3));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(productService, never()).updateStock(any(), any());
    }

    @Test
    @DisplayName("POST /{id}/stock/decrement - decreases stock")
    void decrementStock_success() {
        product.setStockQuantity(6);
        when(productService.decrementStock(1L, 4)).thenReturn(product);

        ResponseEntity<?> response = productController.decrementStock(1L, Map.of("quantity", 4));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).decrementStock(1L, 4);
    }

    @Test
    @DisplayName("POST /{id}/stock/decrement - returns 400 for non-positive quantity")
    void decrementStock_badQuantity() {
        ResponseEntity<?> response = productController.decrementStock(1L, Map.of("quantity", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(productService, never()).decrementStock(any(), any());
    }

    @Test
    @DisplayName("POST /{id}/stock/increase - increases stock")
    void increaseStock_success() {
        when(productService.increaseStock(1L, 5)).thenReturn(product);

        ResponseEntity<?> response = productController.increaseStock(1L, Map.of("quantity", 5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).increaseStock(1L, 5);
    }

    @Test
    @DisplayName("DELETE /{id} - admin deletes a product")
    void delete_asAdmin() {
        ResponseEntity<Void> response = productController.delete(1L, "ROLE_ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /{id} - returns 403 for a non-admin")
    void delete_asUser_forbidden() {
        ResponseEntity<Void> response = productController.delete(1L, "USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(productService, never()).delete(any());
    }
}
