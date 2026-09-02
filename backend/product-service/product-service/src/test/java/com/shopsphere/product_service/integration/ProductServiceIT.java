package com.shopsphere.product_service.integration;

import com.shopsphere.product_service.dto.ProductRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.entity.Product;
import com.shopsphere.product_service.repository.CategoryRepository;
import com.shopsphere.product_service.repository.ProductRepository;
import com.shopsphere.product_service.service.ProductService;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceIT extends PostgresTestContainer {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category clothing;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        clothing = categoryRepository.save(Category.builder().name("Clothing").build());
    }

    @Test
    @DisplayName("create - persists product in PostgreSQL and returns it")
    void create_persistsProductInDatabase() {
        ProductRequest request = ProductRequest.builder()
                .name("Denim Jacket")
                .description("Classic jacket")
                .price(new BigDecimal("59.99"))
                .stockQuantity(30)
                .categoryId(clothing.getId())
                .build();

        Product created = productService.create(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Denim Jacket");
        assertThat(created.getPrice()).isEqualByComparingTo("59.99");
        assertThat(created.getCategory().getName()).isEqualTo("Clothing");
    }

    @Test
    @DisplayName("findById - returns product from PostgreSQL")
    void findById_returnsProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("T-Shirt")
                .price(new BigDecimal("19.99"))
                .stockQuantity(50)
                .categoryId(clothing.getId())
                .build();
        Product saved = productService.create(request);

        Product found = productService.findById(saved.getId());

        assertThat(found.getName()).isEqualTo("T-Shirt");
        assertThat(found.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("findById - throws when product not found")
    void findById_notFound_throwsException() {
        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("decrementStock - reduces quantity in PostgreSQL")
    void decrementStock_reducesQuantity() {
        ProductRequest request = ProductRequest.builder()
                .name("Sneakers")
                .price(new BigDecimal("89.99"))
                .stockQuantity(10)
                .categoryId(clothing.getId())
                .build();
        Product saved = productService.create(request);

        Product updated = productService.decrementStock(saved.getId(), 3);

        assertThat(updated.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("decrementStock - throws when insufficient stock")
    void decrementStock_insufficientStock_throwsException() {
        ProductRequest request = ProductRequest.builder()
                .name("Sneakers")
                .price(new BigDecimal("89.99"))
                .stockQuantity(2)
                .categoryId(clothing.getId())
                .build();
        Product saved = productService.create(request);

        assertThatThrownBy(() -> productService.decrementStock(saved.getId(), 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("searchByName - finds products ignoring case")
    void searchByName_caseInsensitive() {
        productService.create(ProductRequest.builder()
                .name("Denim Jacket").price(new BigDecimal("59.99"))
                .stockQuantity(10).categoryId(clothing.getId()).build());
        productService.create(ProductRequest.builder()
                .name("Cotton T-Shirt").price(new BigDecimal("19.99"))
                .stockQuantity(20).categoryId(clothing.getId()).build());

        List<Product> results = productService.searchByName("jacket");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Denim Jacket");
    }
}
