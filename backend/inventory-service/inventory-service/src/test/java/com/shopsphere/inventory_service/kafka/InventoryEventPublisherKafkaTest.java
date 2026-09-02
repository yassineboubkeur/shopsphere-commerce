package com.shopsphere.inventory_service.kafka;

import com.shopsphere.inventory_service.config.KafkaConfig;
import com.shopsphere.inventory_service.event.StockInsufficientEvent;
import com.shopsphere.inventory_service.event.StockUpdatedEvent;
import com.shopsphere.inventory_service.service.InventoryEventPublisher;
import com.shopsphere.testconfig.kafka.PublisherTestConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(PublisherTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {
        KafkaConfig.STOCK_UPDATED_TOPIC,
        KafkaConfig.STOCK_INSUFFICIENT_TOPIC
})
class InventoryEventPublisherKafkaTest {

    @Autowired
    private InventoryEventPublisher publisher;
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
    void publishesStockUpdatedEventToKafkaWithSerializablePayload() throws Exception {
        StockUpdatedEvent event = StockUpdatedEvent.builder()
                .productId(10L)
                .productName("USB-C Cable")
                .previousQuantity(50)
                .newQuantity(45)
                .reservedQuantity(5)
                .build();

        publisher.publishStockUpdated(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.STOCK_UPDATED_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.STOCK_UPDATED_TOPIC);
        assertThat(record.key()).isEqualTo("10");

        StockUpdatedEvent received = objectMapper.readValue(record.value(), StockUpdatedEvent.class);
        assertThat(received.getProductId()).isEqualTo(10L);
        assertThat(received.getProductName()).isEqualTo("USB-C Cable");
        assertThat(received.getPreviousQuantity()).isEqualTo(50);
        assertThat(received.getNewQuantity()).isEqualTo(45);
        assertThat(received.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    void publishesStockInsufficientEventToKafkaWithSerializablePayload() throws Exception {
        StockInsufficientEvent event = StockInsufficientEvent.builder()
                .orderId(201L)
                .productId(10L)
                .productName("USB-C Cable")
                .requestedQuantity(10)
                .availableQuantity(3)
                .build();

        publisher.publishStockInsufficient(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.STOCK_INSUFFICIENT_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.STOCK_INSUFFICIENT_TOPIC);
        assertThat(record.key()).isEqualTo("10");

        StockInsufficientEvent received = objectMapper.readValue(record.value(), StockInsufficientEvent.class);
        assertThat(received.getOrderId()).isEqualTo(201L);
        assertThat(received.getProductId()).isEqualTo(10L);
        assertThat(received.getRequestedQuantity()).isEqualTo(10);
        assertThat(received.getAvailableQuantity()).isEqualTo(3);
    }
}