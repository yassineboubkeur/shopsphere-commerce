package com.shopsphere.order_service.integration;

import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.OrderItem;
import com.shopsphere.order_service.repository.OrderRepository;
import com.shopsphere.testconfig.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryIT extends PostgresTestContainer {

    @MockitoBean
    private com.shopsphere.order_service.event.OrderEventPublisher orderEventPublisher;

    @MockitoBean
    private com.shopsphere.order_service.service.StockService stockService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("save - persists order with items in PostgreSQL")
    void save_persistsOrderWithItems() {
        Order order = buildOrder("ORD-IT-001", 4L, new BigDecimal("119.98"));
        OrderItem item = OrderItem.builder()
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .subtotal(new BigDecimal("119.98"))
                .build();
        order.setItems(List.of(item));
        item.setOrder(order);

        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-IT-001");
    }

    @Test
    @DisplayName("findByUserId - returns only that user's orders")
    void findByUserId_filtersCorrectly() {
        orderRepository.save(buildOrder("ORD-IT-001", 4L, new BigDecimal("50.00")));
        orderRepository.save(buildOrder("ORD-IT-002", 4L, new BigDecimal("30.00")));
        orderRepository.save(buildOrder("ORD-IT-003", 5L, new BigDecimal("20.00")));

        List<Order> orders = orderRepository.findByUserId(4L);

        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getUserId().equals(4L));
    }

    @Test
    @DisplayName("updateStatus - changes order status in PostgreSQL")
    void updateStatus_persistsChange() {
        Order saved = orderRepository.save(buildOrder("ORD-IT-001", 4L, new BigDecimal("50.00")));
        assertThat(saved.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        saved.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(saved);

        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    private Order buildOrder(String orderNumber, Long userId, BigDecimal amount) {
        return Order.builder()
                .userId(userId)
                .orderNumber(orderNumber)
                .totalAmount(amount)
                .status(Order.OrderStatus.PENDING)
                .build();
    }
}
