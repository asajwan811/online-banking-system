package com.bank.service;

import com.bank.domain.Account;
import com.bank.domain.Transaction;
import com.bank.domain.User;
import com.bank.dto.TransactionDTO;
import com.bank.dto.TransferRequest;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientBalanceException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFullName("Test User");

        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setAccountNumber("1234567890123456");
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setStatus(Account.AccountStatus.ACTIVE);
        fromAccount.setUser(user);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountNumber("9876543210987654");
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setStatus(Account.AccountStatus.ACTIVE);
        toAccount.setUser(user);
    }

    @Test
    void transferFunds_Success() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("1234567890123456");
        request.setToAccountNumber("9876543210987654");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Test transfer");

        when(accountRepository.findByAccountNumber("1234567890123456")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("9876543210987654")).thenReturn(Optional.of(toAccount));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        savedTransaction.setTransactionRef("TXN123");
        savedTransaction.setFromAccount(fromAccount);
        savedTransaction.setToAccount(toAccount);
        savedTransaction.setAmount(new BigDecimal("200.00"));
        savedTransaction.setType(Transaction.TransactionType.TRANSFER);
        savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionDTO result = transactionService.transferFunds(request);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(new BigDecimal("800.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void transferFunds_InsufficientBalance_ThrowsException() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("1234567890123456");
        request.setToAccountNumber("9876543210987654");
        request.setAmount(new BigDecimal("2000.00"));

        when(accountRepository.findByAccountNumber("1234567890123456")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("9876543210987654")).thenReturn(Optional.of(toAccount));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(toAccount));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transferFunds(request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferFunds_AccountNotFound_ThrowsException() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("0000000000000000");
        request.setToAccountNumber("9876543210987654");
        request.setAmount(new BigDecimal("100.00"));

        when(accountRepository.findByAccountNumber("0000000000000000")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> transactionService.transferFunds(request));
    }
}
