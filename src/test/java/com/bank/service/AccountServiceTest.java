package com.bank.service;

import com.bank.domain.Account;
import com.bank.domain.User;
import com.bank.dto.AccountDTO;
import com.bank.dto.CreateAccountRequest;
import com.bank.exception.AccountNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFullName("Test User");

        account = new Account();
        account.setId(1L);
        account.setAccountNumber("1234567890123456");
        account.setUser(user);
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setCurrency("USD");
    }

    @Test
    void createAccount_Success() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType("SAVINGS");
        request.setCurrency("USD");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountDTO result = accountService.createAccount(1L, request);

        assertNotNull(result);
        assertEquals("SAVINGS", result.getAccountType());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void getAccountById_NotFound_ThrowsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(99L));
    }

    @Test
    void freezeAccount_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountDTO result = accountService.freezeAccount(1L);

        assertNotNull(result);
        verify(accountRepository).save(any(Account.class));
    }
}
