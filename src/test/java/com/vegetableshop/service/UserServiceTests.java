package com.vegetableshop.service;

import com.vegetableshop.dto.ProfileUpdateRequest;
import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.entity.AuthProvider;
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

import java.util.Optional;

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
        assertTrue(result.isEmailVerified());
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

    @Test
    void updateProfileNormalizesOptionalInformation() {
        User user = activeUser("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("  Nguyễn Văn B  ");
        request.setPhone("   ");
        request.setAddress("  Quận 1  ");

        User result = new UserService(userRepository, passwordEncoder)
            .updateProfile("USER@example.com", request);

        assertEquals("Nguyễn Văn B", result.getFullName());
        assertNull(result.getPhone());
        assertEquals("Quận 1", result.getAddress());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = activeUser("user@example.com");
        user.setPassword("encoded-old");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        UserService service = new UserService(userRepository, passwordEncoder);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.changePassword("user@example.com", "wrong", "newPassword123")
        );
        verify(userRepository, never()).save(user);
    }

    @Test
    void googleLoginCreatesSafeUserWithoutLocalPassword() {
        when(userRepository.findByOauthSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("google@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        User user = new UserService(userRepository, passwordEncoder)
            .upsertGoogleUser("google-subject", " GOOGLE@example.com ", "Google User");

        assertEquals("google@example.com", user.getEmail());
        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("google-subject", user.getOauthSubject());
        assertNull(user.getPassword());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void googleLoginLinksVerifiedEmailWithoutChangingExistingRole() {
        User admin = activeUser("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setPassword("encoded-password");
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setEmailVerified(false);
        when(userRepository.findByOauthSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.save(admin)).thenReturn(admin);

        User result = new UserService(userRepository, passwordEncoder)
            .upsertGoogleUser("google-subject", "admin@example.com", "Ignored Name");

        assertEquals(Role.ADMIN, result.getRole());
        assertEquals(AuthProvider.LOCAL, result.getAuthProvider());
        assertEquals("google-subject", result.getOauthSubject());
        assertEquals("encoded-password", result.getPassword());
        assertTrue(result.isEmailVerified());
    }

    @Test
    void mailRegistrationStartsWithUnverifiedEmail() {
        RegisterRequest request = request("Nguyen Van A", "user@example.com", "password123");
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        User user = new UserService(userRepository, passwordEncoder).register(request, false);

        org.junit.jupiter.api.Assertions.assertFalse(user.isEmailVerified());
        assertTrue(user.isStatus());
    }

    private RegisterRequest request(String fullName, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(password);
        return request;
    }

    private User activeUser(String email) {
        User user = new User();
        user.setFullName("User");
        user.setEmail(email);
        user.setRole(Role.USER);
        user.setStatus(true);
        return user;
    }
}
