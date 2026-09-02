package com.shopsphere.product_service.controller;

import com.shopsphere.product_service.dto.CategoryRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.service.CategoryService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(com.shopsphere.product_service.security.SecurityConfig.class)
class CategoryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .imageUrl("http://example.com/electronics.png")
                .build();
    }

    @Test
    @DisplayName("GET /api/categories - returns all categories")
    void findAll_returnsList() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - returns single category")
    void findById_returnsCategory() throws Exception {
        when(categoryService.findById(1L)).thenReturn(category);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/electronics.png"));
    }

    @Test
    @DisplayName("POST /api/categories - admin creates category returns 201")
    void create_asAdmin_returns201() throws Exception {
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(category);
        CategoryRequest request = CategoryRequest.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build();

        mockMvc.perform(post("/api/categories")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"));

        verify(categoryService).create(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("POST /api/categories - non-admin returns 403")
    void create_asUser_returns403() throws Exception {
        CategoryRequest request = CategoryRequest.builder().name("Electronics").build();

        mockMvc.perform(post("/api/categories")
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).create(any());
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - admin updates category")
    void update_asAdmin_returns200() throws Exception {
        when(categoryService.update(eq(1L), any(CategoryRequest.class))).thenReturn(category);
        CategoryRequest request = CategoryRequest.builder().name("Electronics").build();

        mockMvc.perform(put("/api/categories/1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - admin deletes returns 204")
    void delete_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    @DisplayName("POST /api/categories - invalid body returns 400")
    void create_invalidBody_returns400() throws Exception {
        CategoryRequest request = CategoryRequest.builder().name("").build();

        mockMvc.perform(post("/api/categories")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}