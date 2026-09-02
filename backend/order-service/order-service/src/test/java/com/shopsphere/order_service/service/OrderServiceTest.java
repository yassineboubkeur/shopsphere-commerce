package com.shopsphere.order_service.service;

import com.shopsphere.order_service.dto.OrderItemRequest;
import com.shopsphere.order_service.dto.OrderRequest;
import com.shopsphere.order_service.dto.UpdateOrderRequest;
import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.OrderItem;
import com.shopsphere.order_service.entity.Payment;
import com.shopsphere.order_service.event.OrderEventPublisher;
import com.shopsphere.order_service.repository.OrderRepository;
import com.shopsphere.order_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private StockService stockService;

    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .subtotal(new BigDecimal("119.98"))
                .build();

        order = Order.builder()
                .id(10L)
                .userId(4L)
                .orderNumber("ORD-ABCD1234")
                .shippingName("Yassine Boubkeur")
                .shippingAddress("Hay zhour 12")
                .shippingCity("Bouznika")
                .shippingZip("13100")
                .shippingPhone("+212610350897")
                .items(List.of(item))
                .totalAmount(new BigDecimal("119.98"))
                .status(Order.OrderStatus.PENDING)
                .build();
        item.setOrder(order);
    }

    @Test
    @DisplayName("placeOrder - creates a PENDING order, decrements stock and publishes event")
    void placeOrder_success() {
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(7L)
                .productName("Denim Jacket")
                .price(new BigDecimal("59.99"))
                .quantity(2)
                .build();
        OrderRequest request = OrderRequest.builder()
                .userId(4L)
                .items(List.of(itemRequest))
                .shippingName("Yassine Boubkeur")
                .shippingAddress("Hay zhour 12")
                .shippingCity("Bouznika")
                .shippingZip("13100")
                .shippingPhone("+212610350897")
                .build();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        Order placed = orderService.placeOrder(request);

        assertThat(placed.getTotalAmount()).isEqualByComparingTo("119.98");
        assertThat(placed.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(placed.getOrderNumber()).startsWith("ORD-");
        assertThat(placed.getItems()).hasSize(1);

        verify(stockService).decrementStock(placed);
        verify(orderEventPublisher).publishOrderCreated(any());
    }

    @Test
    @DisplayName("getOrderById - returns the order when present")
    void getOrderById_found() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        Order found = orderService.getOrderById(10L);

        assertThat(found.getOrderNumber()).isEqualTo("ORD-ABCD1234");
    }

    @Test
    @DisplayName("getOrderById - throws when not found")
    void getOrderById_notFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Order not found");
    }

    @Test
    @DisplayName("getOrderByOrderNumber - returns the order")
    void getOrderByOrderNumber_found() {
        when(orderRepository.findByOrderNumber("ORD-ABCD1234")).thenReturn(Optional.of(order));

        Order found = orderService.getOrderByOrderNumber("ORD-ABCD1234");

        assertThat(found.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getOrdersByUserId - returns orders of a user")
    void getOrdersByUserId_success() {
        when(orderRepository.findByUserId(4L)).thenReturn(List.of(order));

        List<Order> orders = orderService.getOrdersByUserId(4L);

        assertThat(orders).hasSize(1);
    }

    @Test
    @DisplayName("updateStatus - CANCELLED restores stock")
    void updateStatus_cancelled_restoresStock() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.updateStatus(10L, Order.OrderStatus.CANCELLED);

        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(stockService).restoreStock(order);
        verify(orderEventPublisher, never()).publishOrderShipped(any());
    }

    @Test
    @DisplayName("updateStatus - SHIPPED publishes a shipped event")
    void updateStatus_shipped_publishesEvent() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.updateStatus(10L, Order.OrderStatus.SHIPPED);

        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
        verify(orderEventPublisher).publishOrderShipped(any());
    }

    @Test
    @DisplayName("updateStatus - DELIVERED publishes a delivered event")
    void updateStatus_delivered_publishesEvent() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.updateStatus(10L, Order.OrderStatus.DELIVERED);

        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        verify(orderEventPublisher).publishOrderDelivered(any());
    }

    @Test
    @DisplayName("confirmOrder - confirms a PENDING order")
    void confirmOrder_success() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order confirmed = orderService.confirmOrder(10L);

        assertThat(confirmed.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirmOrder - throws when not PENDING")
    void confirmOrder_notPending() {
        order.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only PENDING orders can be confirmed");
    }

    @Test
    @DisplayName("completePayment - completes the payment and confirms the order")
    void completePayment_completesAndConfirms() {
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .amount(new BigDecimal("119.98"))
                .status(Payment.PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment completed = orderService.completePayment(10L, "TRX-001");

        assertThat(completed.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
        assertThat(completed.getTransactionId()).isEqualTo("TRX-001");
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("completePayment - throws when payment not found")
    void completePayment_paymentNotFound() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.completePayment(10L, "TRX-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("updateOrder - updates shipping details for owner of a PENDING order")
    void updateOrder_success() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderRequest update = UpdateOrderRequest.builder()
                .shippingAddress("New Address 5")
                .shippingCity("Rabat")
                .build();

        Order updated = orderService.updateOrder(10L, 4L, update);

        assertThat(updated.getShippingAddress()).isEqualTo("New Address 5");
        assertThat(updated.getShippingCity()).isEqualTo("Rabat");
        assertThat(updated.getShippingName()).isEqualTo("Yassine Boubkeur");
    }

    @Test
    @DisplayName("updateOrder - throws when another user tries to update")
    void updateOrder_notOwner() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(10L, 999L, new UpdateOrderRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to update this order");
    }

    @Test
    @DisplayName("updateOrder - throws when the order is not PENDING")
    void updateOrder_notPending() {
        order.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(10L, 4L, new UpdateOrderRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only PENDING orders can be updated");
    }

    @Test
    @DisplayName("deleteOrder - restores stock and removes a PENDING order owned by the user")
    void deleteOrder_success() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(10L, 4L);

        verify(stockService).restoreStock(order);
        verify(orderRepository).delete(order);
    }

    @Test
    @DisplayName("deleteOrder - throws when not the owner")
    void deleteOrder_notOwner() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteOrder(10L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to delete this order");
        verify(orderRepository, never()).delete(any());
    }
}
