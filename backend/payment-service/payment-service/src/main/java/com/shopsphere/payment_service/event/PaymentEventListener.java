package com.shopsphere.payment_service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventListener {

    @EventListener
    public void handlePaymentEvent(PaymentEvent event) {
        switch (event.getEventType()) {
            case "COMPLETED" -> log.info("💰 Payment COMPLETED - Order: {} | Amount: {} | Txn: {}",
                    event.getPayment().getOrderId(),
                    event.getPayment().getAmount(),
                    event.getPayment().getTransactionId());
            case "FAILED" -> log.info("❌ Payment FAILED - Order: {} | Amount: {} | Reason: {}",
                    event.getPayment().getOrderId(),
                    event.getPayment().getAmount(),
                    event.getPayment().getFailureReason());
            case "REFUNDED" -> log.info("💸 Payment REFUNDED - Order: {} | Amount: {}",
                    event.getPayment().getOrderId(),
                    event.getPayment().getAmount());
        }
    }
}
