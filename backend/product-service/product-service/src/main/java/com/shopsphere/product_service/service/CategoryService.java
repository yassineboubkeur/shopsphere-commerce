package com.shopsphere.product_service.service;

import com.shopsphere.product_service.dto.CategoryRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Category name already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();
        return categoryRepository.save(category);
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        Category category = findById(id);
        categoryRepository.delete(category);
    }
}
