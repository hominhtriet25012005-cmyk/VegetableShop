package com.vegetableshop.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validProductRequestPassesValidation() {
        AdminProductRequest request = validProduct();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void negativeProductValuesAndMissingCategoryAreRejected() {
        AdminProductRequest request = validProduct();
        request.setPrice(new BigDecimal("-1"));
        request.setQuantity(-2);
        request.setCategoryId(null);
        assertEquals(3, validator.validate(request).size());
    }

    @Test
    void blankCategoryNameIsRejected() {
        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setName(" ");
        assertEquals(1, validator.validate(request).size());
    }

    private AdminProductRequest validProduct() {
        AdminProductRequest request = new AdminProductRequest();
        request.setName("Cà rốt");
        request.setPrice(new BigDecimal("25000"));
        request.setQuantity(10);
        request.setCategoryId(1L);
        return request;
    }
}
