package com.shopsphere.product_service.controller;

import com.shopsphere.product_service.dto.ProductRequest;
import com.shopsphere.product_service.entity.Category;
import com.shopsphere.product_service.entity.Product;
import com.shopsphere.product_service.service.ProductService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(com.shopsphere.product_service.security.SecurityConfig.class)
class ProductControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(2L).name("Electronics").build();
        product = Product.builder()
                .id(1L)
                .name("Gaming Laptop")
                .description("High-end gaming laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .active(true)
                .category(category)
                .build();
    }

    @Test
    @DisplayName("GET /api/products - returns all products with 200")
    void findAll_returnsList() throws Exception {
        when(productService.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Gaming Laptop"))
                .andExpect(jsonPath("$[0].price").value(1299.99));
    }

    @Test
    @DisplayName("GET /api/products/{id} - returns a single product")
    void findById_returnsProduct() throws Exception {
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.category.name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/products/search?name=laptop - returns matching products")
    void searchByName_returnsList() throws Exception {
        when(productService.searchByName("laptop")).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products/search").param("name", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gaming Laptop"));
    }

    @Test
    @DisplayName("GET /api/products/filter/price - returns filtered products")
    void filterByPrice_returnsList() throws Exception {
        when(productService.filterByPrice(any(), any())).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products/filter/price")
                        .param("minPrice", "100")
                        .param("maxPrice", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].price").value(1299.99));
    }

    @Test
    @DisplayName("GET /api/products/category/{id} - returns products by category")
    void findByCategory_returnsList() throws Exception {
        when(productService.findByCategory(2L)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products/category/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category.id").value(2));
    }

    @Test
    @DisplayName("GET /api/products/paginated - returns paged results")
    void findAllPaginated_returnsPage() throws Exception {
        when(productService.findAllPaginated(0, 10, "id", "asc"))
                .thenReturn(new PageImpl<>(List.of(product)));

        mockMvc.perform(get("/api/products/paginated")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Gaming Laptop"));
    }

    @Test
    @DisplayName("POST /api/products - admin creates product returns 201")
    void create_asAdmin_returns201() throws Exception {
        when(productService.create(any(ProductRequest.class))).thenReturn(product);
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .categoryId(2L)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"));

        verify(productService).create(any(ProductRequest.class));
    }

    @Test
    @DisplayName("POST /api/products - non-admin returns 403")
    void create_asUser_returns403() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .categoryId(2L)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/products - missing header returns 403")
    void create_noRole_returns403() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(10)
                .categoryId(2L)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products - invalid body returns 400")
    void create_invalidBody_returns400() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("")
                .price(null)
                .stockQuantity(null)
                .categoryId(null)
                .build();

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - admin updates product")
    void update_asAdmin_returns200() throws Exception {
        when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(product);
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Laptop")
                .price(new BigDecimal("1399.99"))
                .stockQuantity(15)
                .categoryId(2L)
                .build();

        mockMvc.perform(put("/api/products/1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming Laptop"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - admin deletes returns 204")
    void delete_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock - admin updates stock")
    void updateStock_asAdmin_returns200() throws Exception {
        when(productService.updateStock(1L, 20)).thenReturn(product);

        mockMvc.perform(patch("/api/products/1/stock")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 20}"))
                .andExpect(status().isOk());

        verify(productService).updateStock(1L, 20);
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock - negative quantity returns 400")
    void updateStock_negative_returns400() throws Exception {
        mockMvc.perform(patch("/api/products/1/stock")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products/{id}/stock/decrement - decreases stock")
    void decrementStock_returns200() throws Exception {
        when(productService.decrementStock(1L, 4)).thenReturn(product);

        mockMvc.perform(post("/api/products/1/stock/decrement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 4}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/products/{id}/stock/decrement - zero quantity returns 400")
    void decrementStock_zero_returns400() throws Exception {
        mockMvc.perform(post("/api/products/1/stock/decrement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/products/{id}/stock/increase - increases stock")
    void increaseStock_returns200() throws Exception {
        when(productService.increaseStock(1L, 5)).thenReturn(product);

        mockMvc.perform(post("/api/products/1/stock/increase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isOk());
    }
}