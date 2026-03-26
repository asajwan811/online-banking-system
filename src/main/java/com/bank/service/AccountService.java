package com.bank.service;

import com.bank.domain.Account;
import com.bank.domain.User;
import com.bank.dto.AccountDTO;
import com.bank.dto.CreateAccountRequest;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.UserNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    public AccountDTO createAccount(Long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setUser(user);
        account.setAccountType(Account.AccountType.valueOf(request.getAccountType().toUpperCase()));
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(request.getCurrency());
        account.setStatus(Account.AccountStatus.ACTIVE);

        return mapToDTO(accountRepository.save(account));
    }

    @Cacheable(value = "accounts", key = "#accountId")
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
        return mapToDTO(account);
    }

    @Cacheable(value = "accounts", key = "'number:' + #accountNumber")
    @Transactional(readOnly = true)
    public AccountDTO getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return mapToDTO(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "accounts", key = "#accountId")
    public AccountDTO freezeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
        account.setStatus(Account.AccountStatus.FROZEN);
        return mapToDTO(accountRepository.save(account));
    }

    @CacheEvict(value = "accounts", key = "#accountId")
    public AccountDTO activateAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
        account.setStatus(Account.AccountStatus.ACTIVE);
        return mapToDTO(accountRepository.save(account));
    }

    private String generateAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            accountNumber = String.format("%016d", Math.abs(random.nextLong() % 10000000000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    public AccountDTO mapToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountType(account.getAccountType().name());
        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setStatus(account.getStatus().name());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUserId(account.getUser().getId());
        dto.setOwnerName(account.getUser().getFullName());
        return dto;
    }
}
