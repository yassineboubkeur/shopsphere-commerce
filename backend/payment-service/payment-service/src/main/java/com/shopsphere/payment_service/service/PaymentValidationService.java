package com.shopsphere.payment_service.service;

import com.shopsphere.payment_service.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentValidationService {

    public List<String> validate(PaymentRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getOrderId() == null) {
            errors.add("Order ID is required");
        }
        if (request.getUserId() == null) {
            errors.add("User ID is required");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            errors.add("Amount must be greater than 0");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            errors.add("Payment method is required");
        }
        if (request.getCardNumber() == null || request.getCardNumber().replaceAll("\\s", "").length() < 16) {
            errors.add("Card number must be 16 digits");
        }
        if (request.getCardHolder() == null || request.getCardHolder().isBlank()) {
            errors.add("Card holder is required");
        }
        if (request.getExpiryDate() == null || !request.getExpiryDate().matches("\\d{2}/\\d{2}")) {
            errors.add("Expiry date must be in MM/YY format");
        }
        if (request.getCvv() == null || request.getCvv().length() < 3) {
            errors.add("CVV must be at least 3 digits");
        }

        return errors;
    }

    public boolean isValid(PaymentRequest request) {
        return validate(request).isEmpty();
    }
}
