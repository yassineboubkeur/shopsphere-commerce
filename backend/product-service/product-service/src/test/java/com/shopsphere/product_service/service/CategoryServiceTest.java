package com.shopsphere.product_service.service;

import com.shopsphere.product_service.dto.CategoryRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.repository.CategoryRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(2L).name("Electronics").description("Gadgets").imageUrl("cat.png").build();
    }

    @Test
    @DisplayName("create - persists a new category")
    void create_success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Clothing")
                .description("Apparel")
                .imageUrl("clothing.png")
                .build();
        when(categoryRepository.existsByName("Clothing")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.create(request);

        assertThat(created.getName()).isEqualTo("Clothing");
        assertThat(created.getDescription()).isEqualTo("Apparel");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create - throws when the name already exists")
    void create_duplicateName() {
        CategoryRequest request = CategoryRequest.builder().name("Clothing").build();
        when(categoryRepository.existsByName("Clothing")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Category name already exists");
    }

    @Test
    @DisplayName("findAll - returns all categories")
    void findAll_success() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<Category> categories = categoryService.findAll();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("findById - returns the category when present")
    void findById_found() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        Category found = categoryService.findById(2L);

        assertThat(found.getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("findById - throws when not found")
    void findById_notFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found with id");
    }

    @Test
    @DisplayName("update - updates category fields")
    void update_success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics & More")
                .description("Updated")
                .imageUrl("new.png")
                .build();
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.update(2L, request);

        assertThat(updated.getName()).isEqualTo("Electronics & More");
        assertThat(updated.getDescription()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("delete - removes an existing category")
    void delete_success() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        categoryService.delete(2L);

        verify(categoryRepository).delete(category);
    }
}
