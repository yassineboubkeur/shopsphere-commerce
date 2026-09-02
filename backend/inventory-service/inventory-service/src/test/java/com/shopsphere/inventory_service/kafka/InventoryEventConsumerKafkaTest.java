package com.shopsphere.inventory_service.kafka;

import com.shopsphere.inventory_service.config.KafkaConfig;
import com.shopsphere.inventory_service.event.OrderCreatedEvent;
import com.shopsphere.inventory_service.event.OrderCreatedEvent.OrderItemEvent;
import com.shopsphere.inventory_service.service.InventoryEventPublisher;
import com.shopsphere.inventory_service.service.InventoryService;
import com.shopsphere.testconfig.kafka.ConsumerTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(ConsumerTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = KafkaConfig.ORDER_CREATED_TOPIC)
class InventoryEventConsumerKafkaTest {

    @MockitoBean
    private InventoryService inventoryService;
    @MockitoBean
    private InventoryEventPublisher eventPublisher;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consumesOrderCreatedEventAndReservesStock() throws Exception {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(201L)
                .userId(4L)
                .orderNumber("ORD-2001")
                .totalAmount(new BigDecimal("59.98"))
                .items(List.of(
                        OrderItemEvent.builder()
                                .productId(7L)
                                .productName("USB-C Hub")
                                .price(new BigDecimal("29.99"))
                                .quantity(2)
                                .build()))
                .build();

        kafkaTemplate.send(KafkaConfig.ORDER_CREATED_TOPIC, "201", objectMapper.writeValueAsString(event)).get();

        verify(inventoryService, timeout(10_000)).reserveStock(eq(7L), eq(2));
    }
}