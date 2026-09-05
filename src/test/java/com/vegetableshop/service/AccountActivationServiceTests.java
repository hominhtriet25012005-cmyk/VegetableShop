package com.vegetableshop.service;

import com.vegetableshop.entity.AccountActivationToken;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.InvalidAccountActivationTokenException;
import com.vegetableshop.repository.AccountActivationTokenRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountActivationServiceTests {

    @Mock private AccountActivationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;

    @Test
    void createsRandomTokenButStoresOnlyHash() {
        User user = pendingUser();
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of());

        String rawToken = service().createToken(user);

        ArgumentCaptor<AccountActivationToken> captor = ArgumentCaptor.forClass(AccountActivationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertNotEquals(rawToken, captor.getValue().getTokenHash());
        assertTrue(captor.getValue().getTokenHash().matches("[0-9a-f]{64}"));
        assertTrue(captor.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void resendOnlyCreatesTokenForActiveUnverifiedAccount() {
        User user = pendingUser();
        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of());

        assertTrue(service().createTokenForUnverifiedEmail(" PENDING@example.com ").isPresent());

        user.setEmailVerified(true);
        assertTrue(service().createTokenForUnverifiedEmail("pending@example.com").isEmpty());
    }

    @Test
    void activationVerifiesEmailAndConsumesToken() {
        User user = pendingUser();
        AccountActivationToken token = validToken(user);
        when(tokenRepository.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(token));

        User activated = service().activate("a".repeat(43));

        assertTrue(activated.isEmailVerified());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void malformedOrExpiredTokenIsRejected() {
        assertThrows(InvalidAccountActivationTokenException.class, () -> service().activate("bad"));
        verify(tokenRepository, never()).findForUpdateByTokenHash(anyString());

        User user = pendingUser();
        AccountActivationToken expired = validToken(user);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(expired));
        assertThrows(InvalidAccountActivationTokenException.class, () -> service().activate("a".repeat(43)));
        assertFalse(user.isEmailVerified());
    }

    private AccountActivationService service() {
        return new AccountActivationService(tokenRepository, userRepository, 24);
    }

    private User pendingUser() {
        User user = new User();
        user.setEmail("pending@example.com");
        user.setFullName("Pending User");
        user.setStatus(true);
        user.setEmailVerified(false);
        return user;
    }

    private AccountActivationToken validToken(User user) {
        AccountActivationToken token = new AccountActivationToken();
        token.setUser(user);
        token.setTokenHash("a".repeat(64));
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        return token;
    }
}
