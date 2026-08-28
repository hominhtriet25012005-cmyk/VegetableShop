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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadsRoleAndAccountStatusFromDatabaseUser() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("$2a$10$hash");
        user.setRole(Role.ADMIN);
        user.setStatus(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(userRepository)
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
            () -> new DatabaseUserDetailsService(userRepository).loadUserByUsername("missing@example.com")
        );
    }
}
