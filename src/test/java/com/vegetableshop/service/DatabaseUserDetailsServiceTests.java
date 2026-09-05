package com.vegetableshop.service;

import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void loadsRoleAndAccountStatusFromDatabaseUser() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("$2a$10$hash");
        user.setRole(Role.ADMIN);
        user.setStatus(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(userRepository, passwordEncoder)
            .loadUserByUsername("  admin@example.com  ");

        assertEquals("admin@example.com", details.getUsername());
        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void throwsStandardSecurityExceptionWhenEmailDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(
            UsernameNotFoundException.class,
            () -> new DatabaseUserDetailsService(userRepository, passwordEncoder)
                .loadUserByUsername("missing@example.com")
        );
    }

    @Test
    void googleOnlyUserReceivesNonNullUnusableLocalPassword() {
        User user = new User();
        user.setEmail("google@example.com");
        user.setPassword(null);
        user.setRole(Role.USER);
        user.setStatus(true);
        when(userRepository.findByEmailIgnoreCase("google@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-sentinel");

        UserDetails details = new DatabaseUserDetailsService(userRepository, passwordEncoder)
            .loadUserByUsername("google@example.com");

        assertEquals("encoded-random-sentinel", details.getPassword());
    }

    @Test
    void unverifiedEmailCannotLogIn() {
        User user = new User();
        user.setEmail("pending@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setStatus(true);
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(userRepository, passwordEncoder)
            .loadUserByUsername("pending@example.com");

        assertFalse(details.isEnabled());
    }
}
