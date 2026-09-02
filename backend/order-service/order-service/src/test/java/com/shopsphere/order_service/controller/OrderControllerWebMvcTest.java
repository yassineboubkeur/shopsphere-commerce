package com.shopsphere.order_service.controller;

import com.shopsphere.order_service.dto.OrderItemRequest;
import com.shopsphere.order_service.dto.OrderRequest;
import com.shopsphere.order_service.dto.UpdateOrderRequest;
import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.OrderItem;
import com.shopsphere.order_service.entity.Payment;
import com.shopsphere.order_service.service.OrderService;
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

@WebMvcTest(OrderController.class)
@Import(com.shopsphere.order_service.security.SecurityConfig.class)
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
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
                .id(1L)
                .userId(4L)
                .orderNumber("ORD-0001")
                .shippingName("Yassine Boubkeur")
                .shippingAddress("Hay zhour 12")
                .shippingCity("Bouznika")
                .shippingZip("13100")
                .shippingPhone("+212610350897")
                .totalAmount(new BigDecimal("119.98"))
                .status(Order.OrderStatus.PENDING)
                .items(List.of(item))
                .build();
    }

    @Test
    @DisplayName("GET /api/orders - admin returns all orders")
    void getAllOrders_asAdmin_returnsList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-0001"))
                .andExpect(jsonPath("$[0].totalAmount").value(119.98));
    }

    @Test
    @DisplayName("GET /api/orders - non-admin returns 403")
    void getAllOrders_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/orders - places order")
    void placeOrder_returnsOrder() throws Exception {
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(order);
        OrderRequest request = OrderRequest.builder()
                .userId(4L)
                .items(List.of(OrderItemRequest.builder()
                        .productId(7L)
                        .productName("Denim Jacket")
                        .price(new BigDecimal("59.99"))
                        .quantity(2)
                        .build()))
                .shippingName("Yassine Boubkeur")
                .shippingAddress("Hay zhour 12")
                .shippingCity("Bouznika")
                .shippingZip("13100")
                .shippingPhone("+212610350897")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-0001"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - returns order by id")
    void getOrderById_returnsOrder() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(order);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(4));
    }

    @Test
    @DisplayName("GET /api/orders/number/{orderNumber} - returns order by number")
    void getOrderByOrderNumber_returnsOrder() throws Exception {
        when(orderService.getOrderByOrderNumber("ORD-0001")).thenReturn(order);

        mockMvc.perform(get("/api/orders/number/ORD-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-0001"));
    }

    @Test
    @DisplayName("GET /api/orders/user/{userId} - returns orders by user id")
    void getOrdersByUserId_returnsList() throws Exception {
        when(orderService.getOrdersByUserId(4L)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/user/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(4));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status - admin updates status")
    void updateStatus_asAdmin_returns200() throws Exception {
        Order confirmed = Order.builder().id(1L).orderNumber("ORD-0001")
                .status(Order.OrderStatus.CONFIRMED).totalAmount(order.getTotalAmount()).build();
        when(orderService.updateStatus(1L, Order.OrderStatus.CONFIRMED)).thenReturn(confirmed);

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status - non-admin returns 403")
    void updateStatus_asUser_returns403() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/confirm - confirms order")
    void confirmOrder_returns200() throws Exception {
        Order confirmed = Order.builder().id(1L).orderNumber("ORD-0001")
                .status(Order.OrderStatus.CONFIRMED).totalAmount(order.getTotalAmount()).build();
        when(orderService.confirmOrder(1L)).thenReturn(confirmed);

        mockMvc.perform(post("/api/orders/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/orders/{id}/payment/complete - completes payment")
    void completePayment_returns200() throws Exception {
        Payment payment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("119.98"))
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId("TXN-001")
                .build();
        when(orderService.completePayment(1L, "TXN-001")).thenReturn(payment);

        mockMvc.perform(post("/api/orders/1/payment/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\": \"TXN-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-001"));
    }

    @Test
    @DisplayName("PUT /api/orders/{id}?userId=x - updates order")
    void updateOrder_returns200() throws Exception {
        when(orderService.updateOrder(eq(1L), eq(4L), any(UpdateOrderRequest.class))).thenReturn(order);
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .shippingName("Yassine Boubkeur")
                .shippingAddress("New Address 45")
                .build();

        mockMvc.perform(put("/api/orders/1")
                        .param("userId", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}?userId=x - deletes order returns 204")
    void deleteOrder_returns204() throws Exception {
        mockMvc.perform(delete("/api/orders/1")
                        .param("userId", "4"))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(1L, 4L);
    }
}