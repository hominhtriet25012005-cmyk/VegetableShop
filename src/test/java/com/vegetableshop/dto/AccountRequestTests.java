package com.vegetableshop.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountRequestTests {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidProfileInformation() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("Nguyễn Văn A");
        request.setPhone("0901 234 567");
        request.setAddress("TP. Hồ Chí Minh");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankNameAndInvalidPhone() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(" ");
        request.setPhone("abc");

        Set<ConstraintViolation<ProfileUpdateRequest>> errors = validator.validate(request);
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("fullName")));
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("phone")));
    }

    @Test
    void rejectsMismatchedChangePasswordConfirmation() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsValidResetPasswordRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("secure-token");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void validatesForgotPasswordEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("not-an-email");

        assertFalse(validator.validate(request).isEmpty());
    }
}
