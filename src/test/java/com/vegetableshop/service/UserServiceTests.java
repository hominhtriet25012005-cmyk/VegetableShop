package com.vegetableshop.service;

import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.DuplicateEmailException;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registerNormalizesInputEncodesPasswordAndForcesUserRole() {
        RegisterRequest request = request("  Nguyen Van A  ", "  USER@Example.COM ", "password123");
        request.setPhone("   ");
        request.setAddress("  TP.HCM  ");
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        User result = new UserService(userRepository, passwordEncoder).register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Nguyen Van A", result.getFullName());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("$2a$10$encoded", result.getPassword());
        assertEquals(Role.USER, result.getRole());
        assertTrue(result.isStatus());
        assertNull(result.getPhone());
        assertEquals("TP.HCM", result.getAddress());
    }

    @Test
    void registerRejectsDuplicateEmailBeforeEncodingOrSaving() {
        RegisterRequest request = request("Nguyen Van A", "used@example.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("used@example.com")).thenReturn(true);

        UserService service = new UserService(userRepository, passwordEncoder);

        assertThrows(DuplicateEmailException.class, () -> service.register(request));
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void createAdminUsesAdminRoleAndDoesNotOverwriteExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("admin@example.com"))
            .thenReturn(false, true);
        when(passwordEncoder.encode("admin12345")).thenReturn("$2a$10$admin");

        UserService service = new UserService(userRepository, passwordEncoder);

        assertTrue(service.createAdminIfMissing("Admin", "ADMIN@example.com", "admin12345"));
        org.junit.jupiter.api.Assertions.assertFalse(
            service.createAdminIfMissing("Admin", "admin@example.com", "admin12345")
        );
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    private RegisterRequest request(String fullName, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(password);
        return request;
    }
}
