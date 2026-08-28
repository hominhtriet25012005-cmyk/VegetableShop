package com.vegetableshop.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validCodRequestPassesValidation() {
        CheckoutRequest request = validRequest();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void blankShippingFieldsAreRejected() {
        CheckoutRequest request = validRequest();
        request.setReceiverName(" ");
        request.setReceiverPhone("abc");
        request.setShippingAddress("");

        Set<ConstraintViolation<CheckoutRequest>> violations = validator.validate(request);
        assertEquals(3, violations.size());
    }

    @Test
    void onlinePaymentCannotBeForgedInPhaseSeven() {
        CheckoutRequest request = validRequest();
        request.setPaymentMethod("VNPAY");
        assertTrue(validator.validate(request).stream()
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("paymentMethod")));
    }

    private CheckoutRequest validRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setReceiverName("Nguyễn Văn An");
        request.setReceiverPhone("0901234567");
        request.setShippingAddress("123 Nguyễn Trãi, TP.HCM");
        request.setPaymentMethod("COD");
        return request;
    }
}
