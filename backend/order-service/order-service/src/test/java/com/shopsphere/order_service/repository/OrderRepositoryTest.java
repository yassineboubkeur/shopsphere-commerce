package com.shopsphere.order_service.repository;

import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.OrderItem;
import com.shopsphere.order_service.entity.Payment;
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
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .userId(4L)
                .orderNumber("ORD-TEST-001")
                .shippingName("Yassine Boubkeur")
                .shippingAddress("Hay zhour 12")
                .shippingCity("Bouznika")
                .shippingZip("13100")
                .shippingPhone("+212610350897")
                .totalAmount(new BigDecimal("119.98"))
                .status(Order.OrderStatus.PENDING)
                .build();
        OrderItem item = OrderItem.builder()
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .subtotal(new BigDecimal("119.98"))
                .build();
        order.setItems(List.of(item));
        item.setOrder(order);
    }

    @Test
    @DisplayName("save - persists an order with its items")
    void saveWithItems() {
        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Denim Jacket");
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-TEST-001");
    }

    @Test
    @DisplayName("findByUserId - returns the orders of a user")
    void findByUserId() {
        orderRepository.save(order);
        orderRepository.save(Order.builder()
                .userId(4L)
                .orderNumber("ORD-TEST-002")
                .totalAmount(new BigDecimal("20.00"))
                .status(Order.OrderStatus.PENDING)
                .build());
        orderRepository.save(Order.builder()
                .userId(5L)
                .orderNumber("ORD-TEST-003")
                .totalAmount(new BigDecimal("30.00"))
                .status(Order.OrderStatus.PENDING)
                .build());

        List<Order> orders = orderRepository.findByUserId(4L);

        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getUserId)
                .containsOnly(4L);
    }

    @Test
    @DisplayName("findByOrderNumber - returns the order")
    void findByOrderNumber() {
        orderRepository.save(order);

        Optional<Order> found = orderRepository.findByOrderNumber("ORD-TEST-001");
        Optional<Order> missing = orderRepository.findByOrderNumber("ORD-NOPE");

        assertThat(found).isPresent();
        assertThat(found.get().getShippingCity()).isEqualTo("Bouznika");
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("saveOrderWithPayment - persists payment linked to the order")
    void saveOrderWithPayment() {
        Order saved = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(saved)
                .amount(new BigDecimal("119.98"))
                .status(Payment.PaymentStatus.COMPLETED)
                .paymentMethod("CREDIT_CARD")
                .transactionId("TXN-TEST-001")
                .build();
        paymentRepository.save(payment);

        Optional<Payment> found = paymentRepository.findByOrderId(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(found.get().getOrder().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByOrderId - empty for an order without payments")
    void findByOrderId_missing() {
        Optional<Payment> found = paymentRepository.findByOrderId(999L);

        assertThat(found).isEmpty();
    }
}