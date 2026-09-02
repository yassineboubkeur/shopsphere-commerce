package com.shopsphere.analytics_service.kafka;

import com.shopsphere.analytics_service.config.KafkaConfig;
import com.shopsphere.analytics_service.entity.AnalyticsEvent;
import com.shopsphere.analytics_service.event.OrderCreatedEvent;
import com.shopsphere.analytics_service.event.OrderCreatedEvent.OrderItemEvent;
import com.shopsphere.analytics_service.event.PaymentFailedEvent;
import com.shopsphere.analytics_service.event.PaymentSuccessfulEvent;
import com.shopsphere.analytics_service.service.AnalyticsService;
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
        KafkaConfig.PAYMENT_FAILED_TOPIC
})
class AnalyticsConsumerKafkaTest {

    @MockitoBean
    private AnalyticsService analyticsService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consumesAllEventTypesAndPersistsAnalyticsEvents() throws Exception {
        OrderCreatedEvent created = OrderCreatedEvent.builder()
                .orderId(401L)
                .userId(4L)
                .orderNumber("ORD-4001")
                .totalAmount(new BigDecimal("99.97"))
                .items(List.of(
                        OrderItemEvent.builder()
                                .productId(1L)
                                .productName("Wireless Mouse")
                                .price(new BigDecimal("49.99"))
                                .quantity(2)
                                .build(),
                        OrderItemEvent.builder()
                                .productId(2L)
                                .productName("Mechanical Keyboard")
                                .price(new BigDecimal("120.00"))
                                .quantity(1)
                                .build()))
                .build();

        PaymentSuccessfulEvent paid = PaymentSuccessfulEvent.builder()
                .orderId(401L)
                .userId(4L)
                .orderNumber("ORD-4001")
                .amount(new BigDecimal("99.97"))
                .paymentMethod("CREDIT_CARD")
                .transactionId("TXN-401")
                .build();

        PaymentFailedEvent failed = PaymentFailedEvent.builder()
                .orderId(402L)
                .userId(4L)
                .orderNumber("ORD-4002")
                .amount(new BigDecimal("19.99"))
                .paymentMethod("DEBIT_CARD")
                .failureReason("INSUFFICIENT_FUNDS")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(KafkaConfig.ORDER_CREATED_TOPIC, "401", objectMapper.writeValueAsString(created)).get();
        kafkaTemplate.send(KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC, "401", objectMapper.writeValueAsString(paid)).get();
        kafkaTemplate.send(KafkaConfig.PAYMENT_FAILED_TOPIC, "402", objectMapper.writeValueAsString(failed)).get();

        ArgumentCaptor<List<AnalyticsEvent>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(analyticsService, timeout(10_000)).saveAll(itemsCaptor.capture());
        List<AnalyticsEvent> items = itemsCaptor.getValue();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(items.get(0).getOrderNumber()).isEqualTo("ORD-4001");
        assertThat(items.get(0).getProductId()).isEqualTo(1L);
        assertThat(items.get(0).getAmount()).isEqualByComparingTo("99.98");
        assertThat(items.get(1).getProductId()).isEqualTo(2L);
        assertThat(items.get(1).getAmount()).isEqualByComparingTo("120.00");

        ArgumentCaptor<AnalyticsEvent> saveCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsService, timeout(10_000).times(2)).save(saveCaptor.capture());
        AnalyticsEvent savedPaid = saveCaptor.getAllValues().stream()
                .filter(e -> "PAYMENT_SUCCESSFUL".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        AnalyticsEvent savedFailed = saveCaptor.getAllValues().stream()
                .filter(e -> "PAYMENT_FAILED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(savedPaid.getOrderNumber()).isEqualTo("ORD-4001");
        assertThat(savedPaid.getStatus()).isEqualTo("PAID");
        assertThat(savedPaid.getAmount()).isEqualByComparingTo("99.97");
        assertThat(savedPaid.getPaymentMethod()).isEqualTo("CREDIT_CARD");

        assertThat(savedFailed.getOrderNumber()).isEqualTo("ORD-4002");
        assertThat(savedFailed.getStatus()).isEqualTo("FAILED");
        assertThat(savedFailed.getPaymentMethod()).isEqualTo("DEBIT_CARD");
    }
}