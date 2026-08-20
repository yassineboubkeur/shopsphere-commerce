package com.shopsphere.order_service.service;

import com.shopsphere.order_service.dto.AddToCartRequest;
import com.shopsphere.order_service.dto.CheckoutResponse;
import com.shopsphere.order_service.dto.UpdateQuantityRequest;
import com.shopsphere.order_service.entity.*;
import com.shopsphere.order_service.repository.CartItemRepository;
import com.shopsphere.order_service.repository.CartRepository;
import com.shopsphere.order_service.repository.OrderRepository;
import com.shopsphere.order_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public Cart validateCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            if (item.getProductId() == null) {
                throw new RuntimeException("Cart item missing product ID");
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Cart item has invalid price: " + item.getProductName());
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Cart item has invalid quantity: " + item.getProductName());
            }
        }

        stockService.validateStock(cart);

        return cart;
    }

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(cart);
                });
    }

    public Cart addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getCartByUserId(userId);

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(Long userId, Long productId, UpdateQuantityRequest request) {
        Cart cart = getCartByUserId(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Product not found in cart"));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    public Cart removeItem(Long userId, Long productId) {
        Cart cart = getCartByUserId(userId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Product not found in cart"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    @Transactional
    public CheckoutResponse checkout(Long userId) {
        Cart cart = validateCart(userId);

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> OrderItem.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .items(orderItems)
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        Order savedOrder = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(savedOrder)
                .amount(totalAmount)
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod("STRIPE")
                .build();
        Payment savedPayment = paymentRepository.save(payment);

        List<CartItem> itemsToRemove = new ArrayList<>(cart.getItems());
        itemsToRemove.forEach(item -> {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        });
        cartRepository.save(cart);

        return CheckoutResponse.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .orderStatus(savedOrder.getStatus().name())
                .totalAmount(savedOrder.getTotalAmount())
                .payment(CheckoutResponse.PaymentInfo.builder()
                        .id(savedPayment.getId())
                        .status(savedPayment.getStatus().name())
                        .paymentMethod(savedPayment.getPaymentMethod())
                        .amount(savedPayment.getAmount())
                        .build())
                .build();
    }
}
