package com.vegetableshop.service;

import com.vegetableshop.entity.PasswordResetToken;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.InvalidPasswordResetTokenException;
import com.vegetableshop.repository.PasswordResetTokenRepository;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserService userService;

    @Test
    void createsRandomTokenButStoresOnlyHashAndInvalidatesOldToken() {
        User user = localUser();
        PasswordResetToken oldToken = validToken(user);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of(oldToken));

        String rawToken = service().createToken(" USER@example.com ").orElseThrow();

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertNotNull(oldToken.getUsedAt());
        assertNotEquals(rawToken, captor.getValue().getTokenHash());
        assertTrue(captor.getValue().getTokenHash().matches("[0-9a-f]{64}"));
        assertTrue(captor.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void doesNotRevealOrCreateTokenForUnknownOrGoogleOnlyAccount() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        User googleUser = localUser();
        googleUser.setPassword(null);
        when(userRepository.findByEmailIgnoreCase("google@example.com")).thenReturn(Optional.of(googleUser));

        assertTrue(service().createToken("missing@example.com").isEmpty());
        assertTrue(service().createToken("google@example.com").isEmpty());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void unverifiedAccountCannotRequestPasswordReset() {
        User user = localUser();
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));

        assertTrue(service().createToken("pending@example.com").isEmpty());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void validatesOnlyUnusedUnexpiredTokenForActiveUser() {
        User user = localUser();
        PasswordResetToken token = validToken(user);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertTrue(service().isValid("a".repeat(43)));

        token.setUsedAt(LocalDateTime.now());
        assertFalse(service().isValid("a".repeat(43)));
        assertFalse(service().isValid(""));
        assertFalse(service().isValid("invalid-token"));
    }

    @Test
    void resetChangesPasswordAndConsumesToken() {
        User user = localUser();
        PasswordResetToken token = validToken(user);
        when(tokenRepository.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(token));

        service().resetPassword("a".repeat(43), "newPassword123");

        verify(userService).setPassword(user, "newPassword123");
        verify(tokenRepository).save(token);
        assertNotNull(token.getUsedAt());
        assertThrows(
            InvalidPasswordResetTokenException.class,
            () -> service().resetPassword("a".repeat(43), "anotherPassword")
        );
    }

    private PasswordResetService service() {
        return new PasswordResetService(userRepository, tokenRepository, userService, 30);
    }

    private User localUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setStatus(true);
        return user;
    }

    private PasswordResetToken validToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("a".repeat(64));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(20));
        return token;
    }
}
