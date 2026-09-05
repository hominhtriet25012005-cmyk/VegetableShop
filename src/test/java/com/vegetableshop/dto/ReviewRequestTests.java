package com.vegetableshop.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewRequestTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void ratingMustBeBetweenOneAndFive() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(6);
        request.setOrderDetailId(1L);

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void validRatingAndCommentPassValidation() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setOrderDetailId(1L);
        request.setComment("Sản phẩm tươi và giao đúng hẹn");

        assertTrue(validator.validate(request).isEmpty());
    }
}
