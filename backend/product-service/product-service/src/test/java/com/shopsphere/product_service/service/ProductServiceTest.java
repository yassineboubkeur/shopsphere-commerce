package com.shopsphere.product_service.service;

import com.shopsphere.product_service.dto.ProductRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.entity.Product;
import com.shopsphere.product_service.repository.CategoryRepository;
import com.shopsphere.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(2L).name("Electronics").build();
        product = Product.builder()
                .id(1L)
                .name("Gaming Laptop")
                .description("Powerful laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .imageUrl("img.png")
                .active(true)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("create - persists a new active product with the given category")
    void create_success() {
        ProductRequest request = ProductRequest.builder()
                .name("Mechanical Keyboard")
                .description("RGB keyboard")
                .price(new BigDecimal("89.00"))
                .stockQuantity(50)
                .categoryId(2L)
                .build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create(request);

        assertThat(created.getName()).isEqualTo("Mechanical Keyboard");
        assertThat(created.getPrice()).isEqualByComparingTo("89.00");
        assertThat(created.getStockQuantity()).isEqualTo(50);
        assertThat(created.getActive()).isTrue();
        assertThat(created.getCategory()).isEqualTo(category);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("create - throws when the category does not exist")
    void create_missingCategory() {
        ProductRequest request = ProductRequest.builder()
                .name("Keyboard")
                .price(new BigDecimal("89.00"))
                .stockQuantity(5)
                .categoryId(999L)
                .build();
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found with id");
    }

    @Test
    @DisplayName("findById - returns the product when present")
    void findById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product found = productService.findById(1L);

        assertThat(found.getName()).isEqualTo("Gaming Laptop");
        assertThat(found.getPrice()).isEqualByComparingTo("1299.99");
    }

    @Test
    @DisplayName("findById - throws when the product does not exist")
    void findById_notFound() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id");
    }

    @Test
    @DisplayName("findAll - returns all products")
    void findAll_success() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> products = productService.findAll();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    @DisplayName("findByCategory - returns products of a category")
    void findByCategory_success() {
        when(productRepository.findByCategoryId(2L)).thenReturn(List.of(product));

        List<Product> products = productService.findByCategory(2L);

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getCategory().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("searchByName - returns matching products")
    void searchByName_success() {
        when(productRepository.findByNameContainingIgnoreCase("laptop")).thenReturn(List.of(product));

        List<Product> products = productService.searchByName("laptop");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    @DisplayName("update - modifies the product fields and category")
    void update_success() {
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop Pro")
                .description("Updated")
                .price(new BigDecimal("1499.99"))
                .stockQuantity(7)
                .imageUrl("img2.png")
                .categoryId(2L)
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.update(1L, request);

        assertThat(updated.getName()).isEqualTo("Gaming Laptop Pro");
        assertThat(updated.getPrice()).isEqualByComparingTo("1499.99");
        assertThat(updated.getStockQuantity()).isEqualTo(7);
        assertThat(updated.getCategory()).isEqualTo(category);
    }

    @Test
    @DisplayName("updateStock - replaces the stock quantity")
    void updateStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.updateStock(1L, 25);

        assertThat(updated.getStockQuantity()).isEqualTo(25);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("decrementStock - decreases the stock")
    void decrementStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.decrementStock(1L, 4);

        assertThat(updated.getStockQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("decrementStock - throws when insufficient stock")
    void decrementStock_insufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decrementStock(1L, 100))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("increaseStock - increases the stock")
    void increaseStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.increaseStock(1L, 5);

        assertThat(updated.getStockQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("filterByPrice - returns products within the price range")
    void filterByPrice_success() {
        when(productRepository.findByPriceBetween(new BigDecimal("10"), new BigDecimal("2000")))
                .thenReturn(List.of(product));

        List<Product> products = productService.filterByPrice(new BigDecimal("10"), new BigDecimal("2000"));

        assertThat(products).hasSize(1);
    }

    @Test
    @DisplayName("delete - removes an existing product")
    void delete_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }
}
