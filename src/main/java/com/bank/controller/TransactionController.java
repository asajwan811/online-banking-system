package com.bank.controller;

import com.bank.dto.DepositWithdrawRequest;
import com.bank.dto.TransactionDTO;
import com.bank.dto.TransferRequest;
import com.bank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.transferFunds(request));
    }

    @PostMapping("/deposit/{accountNumber}")
    @Operation(summary = "Deposit funds into an account")
    public ResponseEntity<TransactionDTO> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositWithdrawRequest request) {
        return ResponseEntity.ok(transactionService.deposit(accountNumber, request));
    }

    @PostMapping("/withdraw/{accountNumber}")
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<TransactionDTO> withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositWithdrawRequest request) {
        return ResponseEntity.ok(transactionService.withdraw(accountNumber, request));
    }

    @GetMapping("/history/{accountNumber}")
    @Operation(summary = "Get transaction history for an account")
    public ResponseEntity<Page<TransactionDTO>> getHistory(
            @PathVariable String accountNumber,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber, pageable));
    }

    @GetMapping("/{transactionRef}")
    @Operation(summary = "Get transaction by reference")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String transactionRef) {
        return ResponseEntity.ok(transactionService.getTransactionByRef(transactionRef));
    }
}
