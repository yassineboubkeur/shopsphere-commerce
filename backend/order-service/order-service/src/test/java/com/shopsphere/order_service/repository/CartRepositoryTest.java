package com.shopsphere.order_service.repository;

import com.shopsphere.order_service.entity.Cart;
import com.shopsphere.order_service.entity.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = Cart.builder().userId(4L).build();
        CartItem item = CartItem.builder()
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .build();
        cart.setItems(List.of(item));
        item.setCart(cart);
    }

    @Test
    @DisplayName("save - persists a cart with its items")
    void saveWithItems() {
        Cart saved = cartRepository.save(cart);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Denim Jacket");
        assertThat(saved.getTotal()).isEqualByComparingTo("119.98");
    }

    @Test
    @DisplayName("findByUserId - returns the cart of a user")
    void findByUserId() {
        cartRepository.save(cart);

        Optional<Cart> found = cartRepository.findByUserId(4L);
        Optional<Cart> missing = cartRepository.findByUserId(99L);

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findByCartIdAndProductId - returns the matching cart item")
    void findByCartIdAndProductId() {
        Cart saved = cartRepository.save(cart);

        Optional<CartItem> item = cartItemRepository.findByCartIdAndProductId(saved.getId(), 7L);
        Optional<CartItem> missing = cartItemRepository.findByCartIdAndProductId(saved.getId(), 123L);

        assertThat(item).isPresent();
        assertThat(item.get().getQuantity()).isEqualTo(2);
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("getTotal - sums item prices times quantities")
    void getTotal() {
        Cart emptyCart = Cart.builder().userId(6L).build();
        cartRepository.save(emptyCart);

        Cart saved = cartRepository.save(cart);

        assertThat(saved.getTotal()).isEqualByComparingTo("119.98");
        assertThat(emptyCart.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}