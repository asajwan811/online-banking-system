package com.bank.service;

import com.bank.domain.User;
import com.bank.exception.UserNotFoundException;
import com.bank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TwoFactorService twoFactorService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setTwoFactorEnabled(false);
    }

    @Test
    void setupTwoFactor_GeneratesQrUrl() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String qrUrl = twoFactorService.setupTwoFactor("testuser");

        assertNotNull(qrUrl);
        assertTrue(qrUrl.startsWith("otpauth://totp/"));
        assertTrue(qrUrl.contains("SecureBank"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void setupTwoFactor_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> twoFactorService.setupTwoFactor("unknown"));
    }

    @Test
    void isTwoFactorEnabled_ReturnsFalseByDefault() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        assertFalse(twoFactorService.isTwoFactorEnabled("testuser"));
    }

    @Test
    void validateCode_WhenTwoFactorDisabled_ReturnsTrue() {
        user.setTwoFactorEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        boolean result = twoFactorService.validateCode("testuser", 000000);
        assertTrue(result);
    }
}
