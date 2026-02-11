package com.company.mts.controller;

import com.company.mts.dto.AccountBalanceDTO;
import com.company.mts.dto.TransferByAccountNumberRequest;
import com.company.mts.entity.Account;
import com.company.mts.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ===============================
    // Create Account
    // ===============================
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(
                request.getHolderName(),
                request.getInitialBalance());
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    // ===============================
    // Get Account by ID
    // ===============================
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Get Account Balance Only
    // ===============================
    @GetMapping("/{id}/balance")
    public ResponseEntity<AccountBalanceDTO> getAccountBalance(@PathVariable Long id) {
        Account account = accountService.getAccount(id);

        AccountBalanceDTO balanceDTO = new AccountBalanceDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getHolderName(),
                account.getBalance(),
                account.getStatus().toString());

        return ResponseEntity.ok(balanceDTO);
    }

    // ===============================
    // Get Account by Account Number
    // ===============================
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<Account> getAccountByNumber(@PathVariable String accountNumber) {
        Account account = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Get Account by Holder Name
    // ===============================
    @GetMapping("/holder/{holderName}")
    public ResponseEntity<Account> getAccountByHolderName(@PathVariable String holderName) {
        Account account = accountService.getAccountByHolderName(holderName);
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Credit Account (Deposit)
    // ===============================
    @PostMapping("/{id}/credit")
    public ResponseEntity<Account> creditAccount(
            @PathVariable Long id,
            @RequestBody AmountRequest request) {

        Account account = accountService.credit(id, request.getAmount());
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Debit Account (Withdraw)
    // ===============================
    @PostMapping("/{id}/debit")
    public ResponseEntity<Account> debitAccount(
            @PathVariable Long id,
            @RequestBody AmountRequest request) {

        Account account = accountService.debit(id, request.getAmount());
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Transfer Between Accounts (by ID) - Legacy endpoint
    // ===============================
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        accountService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount());

        TransferResponse response = new TransferResponse(
                "Transfer successful",
                request.getAmount(),
                true);
        return ResponseEntity.ok(response);
    }

    // ===============================
    // Transfer Between Accounts (by Account Number) - Legacy endpoint
    // ===============================
    @PostMapping("/transfer/by-number")
    public ResponseEntity<TransferResponse> transferByAccountNumber(
            @RequestBody TransferByAccountNumberRequest request) {

        accountService.transferByAccountNumber(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount());

        TransferResponse response = new TransferResponse(
                "Transfer successful",
                request.getAmount(),
                true);
        return ResponseEntity.ok(response);
    }

    // ===============================
    // Lock Account
    // ===============================
    @PostMapping("/{id}/lock")
    public ResponseEntity<Account> lockAccount(@PathVariable Long id) {
        Account account = accountService.lockAccount(id);
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Unlock Account
    // ===============================
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Account> unlockAccount(@PathVariable Long id) {
        Account account = accountService.unlockAccount(id);
        return ResponseEntity.ok(account);
    }

    // ===============================
    // Close Account
    // ===============================
    @PostMapping("/{id}/close")
    public ResponseEntity<Account> closeAccount(@PathVariable Long id) {
        Account account = accountService.closeAccount(id);
        return ResponseEntity.ok(account);
    }

    // Inner class for transfer response
    public static class TransferResponse {
        private String message;
        private java.math.BigDecimal amount;
        private boolean success;

        public TransferResponse(String message, java.math.BigDecimal amount, boolean success) {
            this.message = message;
            this.amount = amount;
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
}