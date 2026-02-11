package com.company.mts.controller;

import com.company.mts.dto.IdempotentTransferRequest;
import com.company.mts.dto.TransactionDTO;
import com.company.mts.dto.TransferByAccountNumberRequest;
import com.company.mts.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransactionService transactionService;

    public TransferController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Execute idempotent transfer
     * POST /api/v1/transfers/idempotent
     */
    @PostMapping("/idempotent")
    public ResponseEntity<TransferResponse> executeIdempotentTransfer(
            @Valid @RequestBody IdempotentTransferRequest request) {

        TransactionDTO transaction = transactionService.executeIdempotentTransfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getIdempotencyKey(),
                request.getDescription());

        TransferResponse response = new TransferResponse(
                "Transfer completed successfully",
                transaction.getId(),
                transaction.getAmount(),
                transaction.getStatus().toString(),
                transaction.getIdempotencyKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Execute transfer using account numbers
     * POST /api/v1/transfers/by-account
     */
    @PostMapping("/by-account")
    public ResponseEntity<TransferResponse> executeTransferByAccountNumber(
            @Valid @RequestBody TransferByAccountNumberRequest request) {

        TransactionDTO transaction = transactionService.executeTransferByAccountNumber(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                "Transfer from frontend");

        TransferResponse response = new TransferResponse(
                "Transfer completed successfully",
                transaction.getId(),
                transaction.getAmount(),
                transaction.getStatus().toString(),
                null);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all transactions
     * GET /api/v1/transfers
     */
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        List<TransactionDTO> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transaction by ID
     * GET /api/v1/transfers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
        TransactionDTO transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Get transaction history for an account
     * GET /api/v1/transfers/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<TransactionHistoryResponse> getAccountTransactionHistory(
            @PathVariable Long accountId) {

        List<TransactionDTO> transactions = transactionService.getAccountTransactionHistory(accountId);

        TransactionHistoryResponse response = new TransactionHistoryResponse(
                accountId,
                transactions.size(),
                transactions);

        return ResponseEntity.ok(response);
    }

    /**
     * Get failed transactions for an account
     * GET /api/v1/transfers/account/{accountId}/failed
     */
    @GetMapping("/account/{accountId}/failed")
    public ResponseEntity<List<TransactionDTO>> getFailedTransactions(@PathVariable Long accountId) {
        List<TransactionDTO> transactions = transactionService.getFailedTransactions(accountId);
        return ResponseEntity.ok(transactions);
    }

    // ===============================
    // Response DTOs
    // ===============================

    public static class TransferResponse {
        private String message;
        private Long transactionId;
        private java.math.BigDecimal amount;
        private String status;
        private String idempotencyKey;

        public TransferResponse(String message, Long transactionId, java.math.BigDecimal amount,
                String status, String idempotencyKey) {
            this.message = message;
            this.transactionId = transactionId;
            this.amount = amount;
            this.status = status;
            this.idempotencyKey = idempotencyKey;
        }

        // Getters and Setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }
    }

    public static class TransactionHistoryResponse {
        private Long accountId;
        private int totalTransactions;
        private List<TransactionDTO> transactions;

        public TransactionHistoryResponse(Long accountId, int totalTransactions,
                List<TransactionDTO> transactions) {
            this.accountId = accountId;
            this.totalTransactions = totalTransactions;
            this.transactions = transactions;
        }

        // Getters and Setters
        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public int getTotalTransactions() {
            return totalTransactions;
        }

        public void setTotalTransactions(int totalTransactions) {
            this.totalTransactions = totalTransactions;
        }

        public List<TransactionDTO> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<TransactionDTO> transactions) {
            this.transactions = transactions;
        }
    }
}