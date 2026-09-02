package com.shopsphere.order_service.kafka;

import com.shopsphere.order_service.config.KafkaConfig;
import com.shopsphere.order_service.event.OrderCreatedEvent;
import com.shopsphere.order_service.event.OrderDeliveredEvent;
import com.shopsphere.order_service.event.OrderEventPublisher;
import com.shopsphere.order_service.event.OrderShippedEvent;
import com.shopsphere.testconfig.kafka.KafkaTestConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(KafkaTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaConfig.ORDER_CREATED_TOPIC,
        KafkaConfig.ORDER_SHIPPED_TOPIC,
        KafkaConfig.ORDER_DELIVERED_TOPIC
})
class OrderEventPublisherKafkaTest {

    @Autowired
    private OrderEventPublisher publisher;
    @Autowired
    private ConsumerFactory<String, String> consumerFactory;
    @Autowired
    private ObjectMapper objectMapper;

    private ConsumerRecord<String, String> getSingleRecord(String topic) {
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(List.of(topic));
            return KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(10));
        }
    }

    @Test
    void publishesOrderCreatedEventToKafkaWithSerializablePayload() throws Exception {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(101L)
                .userId(4L)
                .orderNumber("ORD-1001")
                .totalAmount(new BigDecimal("99.99"))
                .items(List.of(
                        OrderCreatedEvent.OrderItemEvent.builder()
                                .productId(1L)
                                .productName("Wireless Mouse")
                                .price(new BigDecimal("49.99"))
                                .quantity(2)
                                .build(),
                        OrderCreatedEvent.OrderItemEvent.builder()
                                .productId(2L)
                                .productName("Mechanical Keyboard")
                                .price(new BigDecimal("120.00"))
                                .quantity(1)
                                .build()))
                .build();

        publisher.publishOrderCreated(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.ORDER_CREATED_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.ORDER_CREATED_TOPIC);
        assertThat(record.key()).isEqualTo("101");

        OrderCreatedEvent received = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
        assertThat(received.getOrderId()).isEqualTo(101L);
        assertThat(received.getUserId()).isEqualTo(4L);
        assertThat(received.getOrderNumber()).isEqualTo("ORD-1001");
        assertThat(received.getTotalAmount()).isEqualByComparingTo("99.99");
        assertThat(received.getItems()).hasSize(2);
        assertThat(received.getItems().get(0).getProductName()).isEqualTo("Wireless Mouse");
        assertThat(received.getItems().get(0).getPrice()).isEqualByComparingTo("49.99");
    }

    @Test
    void publishesOrderShippedEventToKafkaWithSerializablePayload() throws Exception {
        OrderShippedEvent event = OrderShippedEvent.builder()
                .orderId(102L)
                .userId(4L)
                .orderNumber("ORD-1002")
                .trackingNumber("TRK-ABC-123")
                .carrier("DHL")
                .estimatedDelivery(LocalDateTime.now().plusDays(3))
                .build();

        publisher.publishOrderShipped(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.ORDER_SHIPPED_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.ORDER_SHIPPED_TOPIC);
        assertThat(record.key()).isEqualTo("102");

        OrderShippedEvent received = objectMapper.readValue(record.value(), OrderShippedEvent.class);
        assertThat(received.getOrderId()).isEqualTo(102L);
        assertThat(received.getOrderNumber()).isEqualTo("ORD-1002");
        assertThat(received.getTrackingNumber()).isEqualTo("TRK-ABC-123");
        assertThat(received.getCarrier()).isEqualTo("DHL");
        assertThat(received.getEstimatedDelivery()).isNotNull();
    }

    @Test
    void publishesOrderDeliveredEventToKafkaWithSerializablePayload() throws Exception {
        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .orderId(103L)
                .userId(4L)
                .orderNumber("ORD-1003")
                .deliveredAt(LocalDateTime.now())
                .build();

        publisher.publishOrderDelivered(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.ORDER_DELIVERED_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.ORDER_DELIVERED_TOPIC);
        assertThat(record.key()).isEqualTo("103");

        OrderDeliveredEvent received = objectMapper.readValue(record.value(), OrderDeliveredEvent.class);
        assertThat(received.getOrderId()).isEqualTo(103L);
        assertThat(received.getOrderNumber()).isEqualTo("ORD-1003");
        assertThat(received.getDeliveredAt()).isNotNull();
    }
}