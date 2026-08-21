package com.shopsphere.inventory_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String STOCK_UPDATED_TOPIC = "stock-updated";
    public static final String STOCK_INSUFFICIENT_TOPIC = "stock-insufficient";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockUpdatedTopic() {
        return TopicBuilder.name(STOCK_UPDATED_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockInsufficientTopic() {
        return TopicBuilder.name(STOCK_INSUFFICIENT_TOPIC).partitions(1).replicas(1).build();
    }
}
