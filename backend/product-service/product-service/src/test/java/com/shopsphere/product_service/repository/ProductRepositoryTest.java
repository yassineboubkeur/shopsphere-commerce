package com.shopsphere.product_service.repository;

import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category clothing;
    private Category electronics;

    @BeforeEach
    void setUp() {
        clothing = categoryRepository.save(Category.builder().name("Clothing").build());
        electronics = categoryRepository.save(Category.builder().name("Electronics").build());
    }

    @Test
    @DisplayName("save - persists a product with its category")
    void save() {
        Product product = productRepository.save(buildJacket());

        assertThat(product.getId()).isNotNull();
        assertThat(product.getCategory().getName()).isEqualTo("Clothing");
    }

    @Test
    @DisplayName("findByCategoryId - returns only products of the category")
    void findByCategoryId() {
        productRepository.save(buildJacket());
        productRepository.save(Product.builder()
                .name("Headphones")
                .price(new BigDecimal("89.99"))
                .stockQuantity(12)
                .active(true)
                .category(electronics)
                .build());

        List<Product> clothingProducts = productRepository.findByCategoryId(clothing.getId());
        List<Product> electronicsProducts = productRepository.findByCategoryId(electronics.getId());

        assertThat(clothingProducts).extracting(Product::getName).containsExactly("Denim Jacket");
        assertThat(electronicsProducts).extracting(Product::getName).containsExactly("Headphones");
    }

    @Test
    @DisplayName("findByActiveTrue - returns only active products")
    void findByActiveTrue() {
        productRepository.save(buildJacket());
        productRepository.save(Product.builder()
                .name("Retired Item")
                .price(new BigDecimal("9.99"))
                .stockQuantity(1)
                .active(false)
                .category(clothing)
                .build());

        List<Product> active = productRepository.findByActiveTrue();

        assertThat(active).extracting(Product::getName).containsExactly("Denim Jacket");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase - matches names ignoring case")
    void findByNameContainingIgnoreCase() {
        productRepository.save(buildJacket());

        List<Product> byLower = productRepository.findByNameContainingIgnoreCase("jacket");
        List<Product> byUpper = productRepository.findByNameContainingIgnoreCase("JACKE");
        List<Product> none = productRepository.findByNameContainingIgnoreCase("phone");

        assertThat(byLower).hasSize(1);
        assertThat(byUpper).hasSize(1);
        assertThat(none).isEmpty();
    }

    @Test
    @DisplayName("findByPriceBetween - returns products in the price range")
    void findByPriceBetween() {
        productRepository.save(buildJacket());

        List<Product> inRange = productRepository.findByPriceBetween(new BigDecimal("50.00"), new BigDecimal("70.00"));
        List<Product> outOfRange = productRepository.findByPriceBetween(new BigDecimal("1.00"), new BigDecimal("10.00"));

        assertThat(inRange).extracting(Product::getName).containsExactly("Denim Jacket");
        assertThat(outOfRange).isEmpty();
    }

    private Product buildJacket() {
        return Product.builder()
                .name("Denim Jacket")
                .description("Classic denim jacket")
                .price(new BigDecimal("59.99"))
                .stockQuantity(30)
                .imageUrl("http://localhost:8082/images/jacket.png")
                .active(true)
                .category(clothing)
                .build();
    }
}