package com.shopsphere.payment_service.kafka;

import com.shopsphere.payment_service.config.KafkaConfig;
import com.shopsphere.payment_service.event.PaymentEventPublisher;
import com.shopsphere.payment_service.event.PaymentFailedEvent;
import com.shopsphere.payment_service.event.PaymentSuccessfulEvent;
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
        KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC,
        KafkaConfig.PAYMENT_FAILED_TOPIC
})
class PaymentEventPublisherKafkaTest {

    @Autowired
    private PaymentEventPublisher publisher;
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
    void publishesPaymentSuccessfulEventToKafkaWithSerializablePayload() throws Exception {
        PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder()
                .paymentId(501L)
                .orderId(101L)
                .userId(4L)
                .orderNumber("ORD-1001")
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .transactionId("TXN-777")
                .build();

        publisher.publishPaymentSuccessful(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC);
        assertThat(record.key()).isEqualTo("101");

        PaymentSuccessfulEvent received = objectMapper.readValue(record.value(), PaymentSuccessfulEvent.class);
        assertThat(received.getPaymentId()).isEqualTo(501L);
        assertThat(received.getOrderId()).isEqualTo(101L);
        assertThat(received.getOrderNumber()).isEqualTo("ORD-1001");
        assertThat(received.getAmount()).isEqualByComparingTo("99.99");
        assertThat(received.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(received.getTransactionId()).isEqualTo("TXN-777");
    }

    @Test
    void publishesPaymentFailedEventToKafkaWithSerializablePayload() throws Exception {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(502L)
                .orderId(101L)
                .userId(4L)
                .orderNumber("ORD-1001")
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .failureReason("INSUFFICIENT_FUNDS")
                .timestamp(LocalDateTime.now())
                .build();

        publisher.publishPaymentFailed(event);

        ConsumerRecord<String, String> record = getSingleRecord(KafkaConfig.PAYMENT_FAILED_TOPIC);

        assertThat(record.topic()).isEqualTo(KafkaConfig.PAYMENT_FAILED_TOPIC);
        assertThat(record.key()).isEqualTo("101");

        PaymentFailedEvent received = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
        assertThat(received.getPaymentId()).isEqualTo(502L);
        assertThat(received.getOrderId()).isEqualTo(101L);
        assertThat(received.getFailureReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(received.getAmount()).isEqualByComparingTo("99.99");
        assertThat(received.getTimestamp()).isNotNull();
    }
}