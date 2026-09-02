package com.shopsphere.order_service.controller;

import com.shopsphere.order_service.dto.OrderRequest;
import com.shopsphere.order_service.dto.UpdateOrderRequest;
import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.Payment;
import com.shopsphere.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(10L)
                .userId(4L)
                .orderNumber("ORD-ABCD1234")
                .shippingName("Yassine Boubkeur")
                .totalAmount(new BigDecimal("119.98"))
                .status(Order.OrderStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("GET /api/orders - admin role returns orders")
    void getAllOrders_adminReturnsOrders() {
        when(orderService.getAllOrders()).thenReturn(List.of(order));

        ResponseEntity<List<Order>> response = orderController.getAllOrders("ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/orders - missing role returns 403")
    void getAllOrders_missingRoleForbidden() {
        ResponseEntity<List<Order>> response = orderController.getAllOrders(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/orders - places an order")
    void placeOrder_returnsOrder() {
        OrderRequest request = OrderRequest.builder().userId(4L).build();
        when(orderService.placeOrder(request)).thenReturn(order);

        ResponseEntity<Order> response = orderController.placeOrder(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("GET /api/orders/{id} - returns the order")
    void getOrderById_returnsOrder() {
        when(orderService.getOrderById(10L)).thenReturn(order);

        ResponseEntity<Order> response = orderController.getOrderById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderNumber()).isEqualTo("ORD-ABCD1234");
    }

    @Test
    @DisplayName("GET /api/orders/number/{number} - returns the order by number")
    void getOrderByOrderNumber_returnsOrder() {
        when(orderService.getOrderByOrderNumber("ORD-ABCD1234")).thenReturn(order);

        ResponseEntity<Order> response = orderController.getOrderByOrderNumber("ORD-ABCD1234");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("GET /api/orders/user/{id} - returns the user's orders")
    void getOrdersByUserId_returnsOrders() {
        when(orderService.getOrdersByUserId(4L)).thenReturn(List.of(order));

        ResponseEntity<List<Order>> response = orderController.getOrdersByUserId(4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status - admin updates the status")
    void updateStatus_adminReturnsOrder() {
        when(orderService.updateStatus(10L, Order.OrderStatus.SHIPPED)).thenReturn(order);

        ResponseEntity<Order> response = orderController.updateStatus(
                10L, "ADMIN", Map.of("status", "SHIPPED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderService).updateStatus(10L, Order.OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status - non-admin role returns 403")
    void updateStatus_nonAdminForbidden() {
        ResponseEntity<Order> response = orderController.updateStatus(
                10L, "USER", Map.of("status", "SHIPPED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/confirm - confirms the order")
    void confirmOrder_returnsOrder() {
        when(orderService.confirmOrder(10L)).thenReturn(order);

        ResponseEntity<Order> response = orderController.confirmOrder(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/payment/complete - completes the payment")
    void completePayment_returnsPayment() {
        Payment payment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("119.98"))
                .transactionId("TRX-001")
                .status(Payment.PaymentStatus.COMPLETED)
                .build();
        when(orderService.completePayment(10L, "TRX-001")).thenReturn(payment);

        ResponseEntity<Payment> response = orderController.completePayment(
                10L, Map.of("transactionId", "TRX-001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("PUT /api/orders/{id} - updates the order")
    void updateOrder_returnsOrder() {
        UpdateOrderRequest request = UpdateOrderRequest.builder().shippingCity("Rabat").build();
        when(orderService.updateOrder(10L, 4L, request)).thenReturn(order);

        ResponseEntity<Order> response = orderController.updateOrder(10L, 4L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} - deletes the order returning 204")
    void deleteOrder_returnsNoContent() {
        ResponseEntity<Void> response = orderController.deleteOrder(10L, 4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(orderService).deleteOrder(10L, 4L);
    }
}
