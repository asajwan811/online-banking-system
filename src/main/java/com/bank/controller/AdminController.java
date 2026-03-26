package com.bank.controller;

import com.bank.domain.Account;
import com.bank.domain.Transaction;
import com.bank.dto.AdminDashboardDTO;
import com.bank.dto.UserDTO;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.UserRepository;
import com.bank.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only management endpoints")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get system-wide dashboard statistics")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        AdminDashboardDTO dashboard = new AdminDashboardDTO();
        dashboard.setTotalUsers(userRepository.count());
        dashboard.setTotalAccounts(accountRepository.count());
        dashboard.setActiveAccounts(accountRepository.countByStatus(Account.AccountStatus.ACTIVE));
        dashboard.setFrozenAccounts(accountRepository.countByStatus(Account.AccountStatus.FROZEN));
        dashboard.setTotalTransactions(transactionRepository.count());
        dashboard.setCompletedTransactions(transactionRepository.countByStatus(Transaction.TransactionStatus.COMPLETED));
        dashboard.setFailedTransactions(transactionRepository.countByStatus(Transaction.TransactionStatus.FAILED));
        dashboard.setTotalMoneyInSystem(accountRepository.sumActiveBalances());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{userId}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<Void> disableUser(@PathVariable Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEnabled(false);
            userRepository.save(user);
        });
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/enable")
    @Operation(summary = "Enable a user account")
    public ResponseEntity<Void> enableUser(@PathVariable Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEnabled(true);
            userRepository.save(user);
        });
        return ResponseEntity.ok().build();
    }
}
