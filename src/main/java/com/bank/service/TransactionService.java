package com.bank.service;

import com.bank.domain.Account;
import com.bank.domain.Transaction;
import com.bank.dto.DepositWithdrawRequest;
import com.bank.dto.TransactionDTO;
import com.bank.dto.TransferRequest;
import com.bank.exception.*;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public TransactionDTO transferFunds(TransferRequest request) {
        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .ifPresent(t -> {
                        throw new DuplicateTransactionException(
                            "Duplicate transaction detected for idempotency key: " + request.getIdempotencyKey());
                    });
        }

        // Pessimistic locking - order accounts by id to prevent deadlock
        Account fromAccount;
        Account toAccount;

        Account tempFrom = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getFromAccountNumber()));
        Account tempTo = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));

        // Always lock in consistent order to prevent deadlock
        if (tempFrom.getId() < tempTo.getId()) {
            fromAccount = accountRepository.findByIdWithLock(tempFrom.getId()).orElseThrow();
            toAccount = accountRepository.findByIdWithLock(tempTo.getId()).orElseThrow();
        } else {
            toAccount = accountRepository.findByIdWithLock(tempTo.getId()).orElseThrow();
            fromAccount = accountRepository.findByIdWithLock(tempFrom.getId()).orElseThrow();
        }

        // Validate account statuses
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Source account is not active: " + request.getFromAccountNumber());
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Destination account is not active: " + request.getToAccountNumber());
        }

        // Validate balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                "Insufficient balance. Available: " + fromAccount.getBalance() + ", Required: " + request.getAmount());
        }

        // Perform transfer
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Create audit record
        Transaction transaction = new Transaction();
        transaction.setTransactionRef(generateTransactionRef());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(request.getAmount());
        transaction.setType(Transaction.TransactionType.TRANSFER);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(request.getDescription());
        transaction.setIdempotencyKey(request.getIdempotencyKey());

        TransactionDTO result = mapToDTO(transactionRepository.save(transaction));
        auditService.log("TRANSFER", "TRANSACTION", null, fromAccount.getUser().getUsername(), "Transfer of " + request.getAmount() + " from " + request.getFromAccountNumber() + " to " + request.getToAccountNumber());
        return result;
    }

    @Transactional
    public TransactionDTO deposit(String accountNumber, DepositWithdrawRequest request) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Account is not active: " + accountNumber);
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionRef(generateTransactionRef());
        transaction.setFromAccount(account);
        transaction.setToAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Deposit");

        TransactionDTO depositResult = mapToDTO(transactionRepository.save(transaction));
        auditService.log("DEPOSIT", "TRANSACTION", null, account.getUser().getUsername(), "Deposit of " + request.getAmount() + " to " + accountNumber);
        return depositResult;
    }

    @Transactional
    public TransactionDTO withdraw(String accountNumber, DepositWithdrawRequest request) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Account is not active: " + accountNumber);
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                "Insufficient balance. Available: " + account.getBalance() + ", Required: " + request.getAmount());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionRef(generateTransactionRef());
        transaction.setFromAccount(account);
        transaction.setToAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setType(Transaction.TransactionType.WITHDRAWAL);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Withdrawal");

        TransactionDTO withdrawResult = mapToDTO(transactionRepository.save(transaction));
        auditService.log("WITHDRAWAL", "TRANSACTION", null, account.getUser().getUsername(), "Withdrawal of " + request.getAmount() + " from " + accountNumber);
        return withdrawResult;
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionHistory(String accountNumber, Pageable pageable) {
        return transactionRepository.findByAccountNumber(accountNumber, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public TransactionDTO getTransactionByRef(String transactionRef) {
        Transaction transaction = transactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionRef));
        return mapToDTO(transaction);
    }

    private String generateTransactionRef() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase();
    }

    public TransactionDTO mapToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setTransactionRef(transaction.getTransactionRef());
        dto.setFromAccountNumber(transaction.getFromAccount().getAccountNumber());
        dto.setToAccountNumber(transaction.getToAccount().getAccountNumber());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType().name());
        dto.setStatus(transaction.getStatus().name());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
}
