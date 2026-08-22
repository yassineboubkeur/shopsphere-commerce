package com.shopsphere.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String PAYMENT_SUCCESSFUL_TOPIC = "payment-successful";
    public static final String ORDER_SHIPPED_TOPIC = "order-shipped";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentSuccessfulTopic() {
        return TopicBuilder.name(PAYMENT_SUCCESSFUL_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name(ORDER_SHIPPED_TOPIC).partitions(1).replicas(1).build();
    }
}
