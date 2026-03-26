package com.bank.controller;

import com.bank.dto.AccountDTO;
import com.bank.dto.CreateAccountRequest;
import com.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private com.bank.service.UserService userService;

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<AccountDTO> createAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAccountRequest request) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(userId, request));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<AccountDTO> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/my-accounts")
    @Operation(summary = "Get all accounts for the authenticated user")
    public ResponseEntity<List<AccountDTO>> getMyAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all accounts for a user (Admin only)")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PutMapping("/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Freeze an account (Admin only)")
    public ResponseEntity<AccountDTO> freezeAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.freezeAccount(accountId));
    }

    @PutMapping("/{accountId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a frozen account (Admin only)")
    public ResponseEntity<AccountDTO> activateAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.activateAccount(accountId));
    }
}
