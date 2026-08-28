package com.vegetableshop.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRegistrationData() {
        RegisterRequest request = validRequest();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMismatchedPasswordAndInvalidEmail() {
        RegisterRequest request = validRequest();
        request.setEmail("email-khong-hop-le");
        request.setConfirmPassword("different-password");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("passwordConfirmed")));
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyễn Văn A");
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setPhone("0901234567");
        request.setAddress("TP.HCM");
        return request;
    }
}
