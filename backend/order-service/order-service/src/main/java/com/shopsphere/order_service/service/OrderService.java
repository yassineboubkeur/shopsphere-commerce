package com.shopsphere.order_service.service;

import com.shopsphere.order_service.dto.OrderItemRequest;
import com.shopsphere.order_service.dto.OrderRequest;
import com.shopsphere.order_service.dto.UpdateOrderRequest;
import com.shopsphere.order_service.entity.Order;
import com.shopsphere.order_service.entity.OrderItem;
import com.shopsphere.order_service.entity.Payment;
import com.shopsphere.order_service.event.OrderCreatedEvent;
import com.shopsphere.order_service.event.OrderDeliveredEvent;
import com.shopsphere.order_service.event.OrderEventPublisher;
import com.shopsphere.order_service.event.OrderShippedEvent;
import com.shopsphere.order_service.repository.OrderRepository;
import com.shopsphere.order_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final StockService stockService;

    public Order placeOrder(OrderRequest request) {
        List<OrderItem> items = request.getItems().stream()
                .map(this::mapToOrderItem)
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(request.getUserId())
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .shippingName(request.getShippingName())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingZip(request.getShippingZip())
                .shippingPhone(request.getShippingPhone())
                .items(items)
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .build();

        items.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);
        stockService.decrementStock(saved);
        orderEventPublisher.publishOrderCreated(toOrderCreatedEvent(saved));
        return saved;
    }

    private OrderCreatedEvent toOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .items(itemEvents)
                .totalAmount(order.getTotalAmount())
                .build();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order updateStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Order.OrderStatus previous = order.getStatus();
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        if (status == Order.OrderStatus.CANCELLED && previous != Order.OrderStatus.CANCELLED) {
            stockService.restoreStock(saved);
        }

        if (status == Order.OrderStatus.SHIPPED) {
            orderEventPublisher.publishOrderShipped(OrderShippedEvent.builder()
                    .orderId(saved.getId())
                    .userId(saved.getUserId())
                    .orderNumber(saved.getOrderNumber())
                    .trackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .carrier("ShopSphere Express")
                    .estimatedDelivery(LocalDateTime.now().plusDays(3))
                    .build());
        } else if (status == Order.OrderStatus.DELIVERED) {
            orderEventPublisher.publishOrderDelivered(OrderDeliveredEvent.builder()
                    .orderId(saved.getId())
                    .userId(saved.getUserId())
                    .orderNumber(saved.getOrderNumber())
                    .deliveredAt(LocalDateTime.now())
                    .build());
        }
        return saved;
    }

    public Order confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be confirmed");
        }

        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Payment completePayment(Long orderId, String transactionId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        return payment;
    }

    @Transactional
    public Order updateOrder(Long orderId, Long userId, UpdateOrderRequest request) {
        Order order = getOrderById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this order");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be updated");
        }
        if (request.getShippingName() != null) order.setShippingName(request.getShippingName());
        if (request.getShippingAddress() != null) order.setShippingAddress(request.getShippingAddress());
        if (request.getShippingCity() != null) order.setShippingCity(request.getShippingCity());
        if (request.getShippingZip() != null) order.setShippingZip(request.getShippingZip());
        if (request.getShippingPhone() != null) order.setShippingPhone(request.getShippingPhone());
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long orderId, Long userId) {
        Order order = getOrderById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this order");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be deleted");
        }
        stockService.restoreStock(order);
        orderRepository.delete(order);
    }

    private OrderItem mapToOrderItem(OrderItemRequest req) {
        return OrderItem.builder()
                .productId(req.getProductId())
                .productName(req.getProductName())
                .price(req.getPrice())
                .quantity(req.getQuantity())
                .subtotal(req.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())))
                .build();
    }
}
