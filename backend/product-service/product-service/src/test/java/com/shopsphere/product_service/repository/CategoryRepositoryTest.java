package com.shopsphere.product_service.repository;

import com.shopsphere.product_service.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("save - persists a category")
    void save() {
        Category category = categoryRepository.save(buildCategory());

        assertThat(category.getId()).isNotNull();
        assertThat(category.getName()).isEqualTo("Clothing");
    }

    @Test
    @DisplayName("findByName - returns the category")
    void findByName() {
        categoryRepository.save(buildCategory());

        Optional<Category> found = categoryRepository.findByName("Clothing");

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Apparel items");
    }

    @Test
    @DisplayName("findByName - empty when the category is missing")
    void findByName_notFound() {
        Optional<Category> found = categoryRepository.findByName("Missing");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByName - true only for a persisted name")
    void existsByName() {
        categoryRepository.save(buildCategory());

        assertThat(categoryRepository.existsByName("Clothing")).isTrue();
        assertThat(categoryRepository.existsByName("Electronics")).isFalse();
    }

    private Category buildCategory() {
        return Category.builder()
                .name("Clothing")
                .description("Apparel items")
                .imageUrl("http://localhost:8082/images/clothing.png")
                .build();
    }
}