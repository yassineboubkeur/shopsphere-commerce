package com.shopsphere.notification_service.kafka;

import com.shopsphere.notification_service.config.KafkaConfig;
import com.shopsphere.notification_service.event.OrderCreatedEvent;
import com.shopsphere.notification_service.event.OrderCreatedEvent.OrderItemEvent;
import com.shopsphere.notification_service.event.OrderDeliveredEvent;
import com.shopsphere.notification_service.event.OrderShippedEvent;
import com.shopsphere.notification_service.event.PaymentFailedEvent;
import com.shopsphere.notification_service.event.PaymentSuccessfulEvent;
import com.shopsphere.notification_service.service.NotificationService;
import com.shopsphere.testconfig.kafka.KafkaTestConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(KafkaTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaConfig.ORDER_CREATED_TOPIC,
        KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC,
        KafkaConfig.ORDER_SHIPPED_TOPIC,
        KafkaConfig.PAYMENT_FAILED_TOPIC,
        KafkaConfig.ORDER_DELIVERED_TOPIC
})
class NotificationConsumerKafkaTest {

    @MockitoBean
    private NotificationService notificationService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consumesAllEventTypesAndDispatchesToNotificationService() throws Exception {
        OrderCreatedEvent created = OrderCreatedEvent.builder()
                .orderId(301L)
                .userId(4L)
                .orderNumber("ORD-3001")
                .totalAmount(new BigDecimal("89.97"))
                .items(List.of(
                        OrderItemEvent.builder()
                                .productId(1L)
                                .productName("Wireless Mouse")
                                .price(new BigDecimal("49.99"))
                                .quantity(2)
                                .build()))
                .build();

        PaymentSuccessfulEvent paid = PaymentSuccessfulEvent.builder()
                .orderId(301L)
                .userId(4L)
                .orderNumber("ORD-3001")
                .amount(new BigDecimal("89.97"))
                .paymentMethod("CREDIT_CARD")
                .transactionId("TXN-301")
                .build();

        OrderShippedEvent shipped = OrderShippedEvent.builder()
                .orderId(301L)
                .userId(4L)
                .orderNumber("ORD-3001")
                .trackingNumber("TRK-301")
                .carrier("DHL")
                .estimatedDelivery(LocalDateTime.now().plusDays(2))
                .build();

        PaymentFailedEvent failed = PaymentFailedEvent.builder()
                .orderId(302L)
                .userId(4L)
                .orderNumber("ORD-3002")
                .amount(new BigDecimal("19.99"))
                .paymentMethod("CREDIT_CARD")
                .failureReason("INSUFFICIENT_FUNDS")
                .timestamp(LocalDateTime.now())
                .build();

        OrderDeliveredEvent delivered = OrderDeliveredEvent.builder()
                .orderId(301L)
                .userId(4L)
                .orderNumber("ORD-3001")
                .deliveredAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(KafkaConfig.ORDER_CREATED_TOPIC, "301", objectMapper.writeValueAsString(created)).get();
        kafkaTemplate.send(KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC, "301", objectMapper.writeValueAsString(paid)).get();
        kafkaTemplate.send(KafkaConfig.ORDER_SHIPPED_TOPIC, "301", objectMapper.writeValueAsString(shipped)).get();
        kafkaTemplate.send(KafkaConfig.PAYMENT_FAILED_TOPIC, "302", objectMapper.writeValueAsString(failed)).get();
        kafkaTemplate.send(KafkaConfig.ORDER_DELIVERED_TOPIC, "301", objectMapper.writeValueAsString(delivered)).get();

        ArgumentCaptor<OrderCreatedEvent> createdCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(notificationService, timeout(10_000)).sendOrderConfirmation(createdCaptor.capture());
        assertThat(createdCaptor.getValue().getOrderNumber()).isEqualTo("ORD-3001");
        assertThat(createdCaptor.getValue().getItems()).hasSize(1);

        ArgumentCaptor<PaymentSuccessfulEvent> paidCaptor = ArgumentCaptor.forClass(PaymentSuccessfulEvent.class);
        verify(notificationService, timeout(10_000)).sendPaymentConfirmation(paidCaptor.capture());
        assertThat(paidCaptor.getValue().getOrderNumber()).isEqualTo("ORD-3001");
        assertThat(paidCaptor.getValue().getTransactionId()).isEqualTo("TXN-301");

        ArgumentCaptor<OrderShippedEvent> shippedCaptor = ArgumentCaptor.forClass(OrderShippedEvent.class);
        verify(notificationService, timeout(10_000)).sendShippingNotification(shippedCaptor.capture());
        assertThat(shippedCaptor.getValue().getTrackingNumber()).isEqualTo("TRK-301");
        assertThat(shippedCaptor.getValue().getCarrier()).isEqualTo("DHL");

        ArgumentCaptor<PaymentFailedEvent> failedCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(notificationService, timeout(10_000)).sendPaymentFailedNotification(failedCaptor.capture());
        assertThat(failedCaptor.getValue().getOrderNumber()).isEqualTo("ORD-3002");
        assertThat(failedCaptor.getValue().getFailureReason()).isEqualTo("INSUFFICIENT_FUNDS");

        ArgumentCaptor<OrderDeliveredEvent> deliveredCaptor = ArgumentCaptor.forClass(OrderDeliveredEvent.class);
        verify(notificationService, timeout(10_000)).sendOrderDeliveredNotification(deliveredCaptor.capture());
        assertThat(deliveredCaptor.getValue().getOrderNumber()).isEqualTo("ORD-3001");
    }
}