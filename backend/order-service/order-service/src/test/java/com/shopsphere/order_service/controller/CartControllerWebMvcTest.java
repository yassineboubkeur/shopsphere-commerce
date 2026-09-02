package com.shopsphere.order_service.controller;

import com.shopsphere.order_service.dto.AddToCartRequest;
import com.shopsphere.order_service.dto.CheckoutResponse;
import com.shopsphere.order_service.dto.UpdateQuantityRequest;
import com.shopsphere.order_service.entity.Cart;
import com.shopsphere.order_service.entity.CartItem;
import com.shopsphere.order_service.service.CartService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@Import(com.shopsphere.order_service.security.SecurityConfig.class)
class CartControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        CartItem item = CartItem.builder()
                .id(1L)
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .build();

        cart = Cart.builder()
                .id(1L)
                .userId(4L)
                .items(List.of(item))
                .build();
    }

    @Test
    @DisplayName("GET /api/cart/{userId} - returns user's cart")
    void getCart_returnsCart() throws Exception {
        when(cartService.getCartByUserId(4L)).thenReturn(cart);

        mockMvc.perform(get("/api/cart/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(4))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Denim Jacket"));
    }

    @Test
    @DisplayName("POST /api/cart/{userId}/items - add item to cart")
    void addToCart_returnsCart() throws Exception {
        when(cartService.addToCart(eq(4L), any(AddToCartRequest.class))).thenReturn(cart);
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/cart/4/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(4));
    }

    @Test
    @DisplayName("PUT /api/cart/{userId}/items/{productId} - update item quantity")
    void updateQuantity_returnsCart() throws Exception {
        when(cartService.updateQuantity(eq(4L), eq(7L), any(UpdateQuantityRequest.class))).thenReturn(cart);
        UpdateQuantityRequest request = UpdateQuantityRequest.builder().quantity(3).build();

        mockMvc.perform(put("/api/cart/4/items/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(4));
    }

    @Test
    @DisplayName("DELETE /api/cart/{userId}/items/{productId} - remove item from cart")
    void removeItem_returnsCart() throws Exception {
        when(cartService.removeItem(4L, 7L)).thenReturn(cart);

        mockMvc.perform(delete("/api/cart/4/items/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(4));
    }

    @Test
    @DisplayName("GET /api/cart/{userId}/validate - validates cart")
    void validateCart_returnsCart() throws Exception {
        when(cartService.validateCart(4L)).thenReturn(cart);

        mockMvc.perform(get("/api/cart/4/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(4));
    }

    @Test
    @DisplayName("POST /api/cart/{userId}/checkout - checkout returns CheckoutResponse")
    void checkout_returnsResponse() throws Exception {
        CheckoutResponse checkoutResponse = CheckoutResponse.builder()
                .orderId(1L)
                .orderNumber("ORD-0001")
                .orderStatus("PENDING")
                .totalAmount(new BigDecimal("119.98"))
                .build();
        when(cartService.checkout(4L)).thenReturn(checkoutResponse);

        mockMvc.perform(post("/api/cart/4/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-0001"))
                .andExpect(jsonPath("$.totalAmount").value(119.98));
    }
}