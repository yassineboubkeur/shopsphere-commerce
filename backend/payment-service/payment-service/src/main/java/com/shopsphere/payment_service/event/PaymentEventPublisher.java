package com.shopsphere.payment_service.event;

import com.shopsphere.payment_service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishPaymentSuccessful(PaymentSuccessfulEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing PaymentSuccessful event: orderId={} | amount={} | method={}",
                    event.getOrderId(), event.getAmount(), event.getPaymentMethod());
            kafkaTemplate.send(KafkaConfig.PAYMENT_SUCCESSFUL_TOPIC, String.valueOf(event.getOrderId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish PaymentSuccessful event: {}", e.getMessage(), e);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info("Publishing PaymentFailed event: orderId={} | reason={}",
                    event.getOrderId(), event.getFailureReason());
            kafkaTemplate.send(KafkaConfig.PAYMENT_FAILED_TOPIC, String.valueOf(event.getOrderId()), json).get();
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailed event: {}", e.getMessage(), e);
        }
    }
}