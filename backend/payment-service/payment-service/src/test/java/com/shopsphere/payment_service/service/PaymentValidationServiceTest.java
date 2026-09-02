package com.shopsphere.payment_service.service;

import com.shopsphere.payment_service.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentValidationServiceTest {

    private PaymentValidationService validationService;
    private PaymentRequest.PaymentRequestBuilder valid;

    @BeforeEach
    void setUp() {
        validationService = new PaymentValidationService();
        valid = PaymentRequest.builder()
                .orderId(10L)
                .userId(4L)
                .amount(new BigDecimal("119.98"))
                .paymentMethod("CREDIT_CARD")
                .cardNumber("4111 1111 1111 1111")
                .cardHolder("Yassine Boubkeur")
                .expiryDate("12/28")
                .cvv("123");
    }

    @Test
    @DisplayName("validate - accepts a fully valid request")
    void validate_validRequestNoErrors() {
        assertThat(validationService.validate(valid.build())).isEmpty();
    }

    @Test
    @DisplayName("validate - rejects missing order id and user id")
    void validate_missingIds() {
        List<String> errors = validationService.validate(valid.orderId(null).userId(null).build());

        assertThat(errors).contains("Order ID is required", "User ID is required");
    }

    @Test
    @DisplayName("validate - rejects non-positive amount")
    void validate_nonPositiveAmount() {
        assertThat(validationService.validate(valid.amount(BigDecimal.ZERO).build()))
                .contains("Amount must be greater than 0");
        assertThat(validationService.validate(valid.amount(new BigDecimal("-5")).build()))
                .contains("Amount must be greater than 0");
    }

    @Test
    @DisplayName("validate - rejects blank payment method")
    void validate_blankPaymentMethod() {
        assertThat(validationService.validate(valid.paymentMethod("  ").build()))
                .contains("Payment method is required");
    }

    @Test
    @DisplayName("validate - rejects short card number, even ignoring spaces")
    void validate_shortCardNumber() {
        assertThat(validationService.validate(valid.cardNumber("1234").build()))
                .contains("Card number must be 16 digits");
        assertThat(validationService.validate(valid.cardNumber("4111 1111 1111").build()))
                .contains("Card number must be 16 digits");
    }

    @Test
    @DisplayName("validate - rejects missing/blank card holder")
    void validate_blankCardHolder() {
        assertThat(validationService.validate(valid.cardHolder("").build()))
                .contains("Card holder is required");
    }

    @Test
    @DisplayName("validate - rejects bad expiry format")
    void validate_badExpiry() {
        assertThat(validationService.validate(valid.expiryDate("2028-12").build()))
                .contains("Expiry date must be in MM/YY format");
    }

    @Test
    @DisplayName("validate - rejects short cvv")
    void validate_shortCvv() {
        assertThat(validationService.validate(valid.cvv("12").build()))
                .contains("CVV must be at least 3 digits");
    }

    @Test
    @DisplayName("isValid - returns true only when there are no errors")
    void isValid_flag() {
        assertThat(validationService.isValid(valid.build())).isTrue();
        assertThat(validationService.isValid(valid.cardNumber("1234").build())).isFalse();
    }
}
