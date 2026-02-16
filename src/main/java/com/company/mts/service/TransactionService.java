package com.company.mts.service;

import com.company.mts.dto.TransactionDTO;
import com.company.mts.entity.*;
import com.company.mts.exception.DuplicateTransactionException;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.BankDetailsRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

        private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

        private final TransactionLogRepository transactionLogRepository;
        private final AccountRepository accountRepository;
        private final BankDetailsRepository bankDetailsRepository;

        public TransactionService(TransactionLogRepository transactionLogRepository,
                        AccountRepository accountRepository,
                        BankDetailsRepository bankDetailsRepository) {
                this.transactionLogRepository = transactionLogRepository;
                this.accountRepository = accountRepository;
                this.bankDetailsRepository = bankDetailsRepository;
        }

        /**
         * Execute transfer with idempotency protection
         */
        @Transactional
        public TransactionDTO executeIdempotentTransfer(Long fromAccountId, Long toAccountId,
                        BigDecimal amount, String idempotencyKey,
                        String description) {
                logger.info("Executing idempotent transfer - Key: {}, From: {}, To: {}, Amount: {}",
                                idempotencyKey, fromAccountId, toAccountId, amount);

                // Check if transaction already exists
                if (transactionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
                        TransactionLog existingTx = transactionLogRepository.findByIdempotencyKey(idempotencyKey)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "Transaction exists but not found"));

                        logger.warn("Duplicate transaction detected - Idempotency Key: {}, Existing TX ID: {}",
                                        idempotencyKey, existingTx.getId());

                        throw new DuplicateTransactionException(
                                        "Transaction with this idempotency key already exists",
                                        existingTx.getId());
                }

                if (fromAccountId.equals(toAccountId)) {
                        throw new IllegalArgumentException("Self-transfer is not allowed.");
                }

                // Get accounts
                Account fromAccount = accountRepository.findById(fromAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Sender account not found with ID: " + fromAccountId));

                Account toAccount = accountRepository.findById(toAccountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Receiver account not found with ID: " + toAccountId));

                // Capture balance snapshots BEFORE transaction
                BigDecimal fromBalanceBefore = fromAccount.getBalance();
                BigDecimal toBalanceBefore = toAccount.getBalance();

                // Create transaction log with PENDING status
                TransactionLog txLog = TransactionLog.builder()
                                .fromAccountId(fromAccountId)
                                .toAccountId(toAccountId)
                                .amount(amount)
                                .type(TransactionType.TRANSFER)
                                .status(TransactionStatus.PENDING)
                                .description(description != null ? description
                                                : "Transfer from account " + fromAccountId + " to " + toAccountId)
                                .fromAccountBalanceBefore(fromBalanceBefore)
                                .toAccountBalanceBefore(toBalanceBefore)
                                .idempotencyKey(idempotencyKey)
                                .transactionDate(LocalDateTime.now())
                                .build();

                try {
                        // Perform the transfer
                        fromAccount.debit(amount);
                        toAccount.credit(amount);

                        // Capture balance snapshots AFTER transaction
                        txLog.setFromAccountBalanceAfter(fromAccount.getBalance());
                        txLog.setToAccountBalanceAfter(toAccount.getBalance());
                        txLog.setStatus(TransactionStatus.SUCCESS);

                        // Save transaction log
                        TransactionLog savedTx = transactionLogRepository.save(txLog);

                        logger.info("Transfer completed successfully - TX ID: {}, Idempotency Key: {}",
                                        savedTx.getId(), idempotencyKey);

                        return convertToDTO(savedTx, fromAccount, toAccount);

                } catch (Exception e) {
                        // Mark transaction as failed
                        txLog.setStatus(TransactionStatus.FAILED);
                        txLog.setFailureReason(e.getMessage());
                        transactionLogRepository.save(txLog);

                        logger.error("Transfer failed - Idempotency Key: {}, Error: {}", idempotencyKey,
                                        e.getMessage());
                        throw e;
                }
        }

        /**
         * Execute transfer using account numbers.
         * Logic:
         * - If toAccountNumber exists: Perform internal transfer (Debit sender, Credit
         * recipient).
         * - If toAccountNumber does not exist: Perform external debit (Debit sender
         * only).
         */
        @Transactional
        public TransactionDTO executeTransferByAccountNumber(String fromAccountNumber, String toAccountNumber,
                        BigDecimal amount, String description) {
                logger.info("Executing transfer by account numbers - From: {}, To: {}, Amount: {}",
                                fromAccountNumber, toAccountNumber, amount);

                if (fromAccountNumber.equals(toAccountNumber)) {
                        throw new IllegalArgumentException("Self-transfer is not allowed.");
                }

                // Smart Provisioning: If sender account doesn't exist in 'accounts' table,
                // check 'bank_details'
                Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                                .orElseGet(() -> {
                                        logger.info("Sender account {} not found in accounts table. Checking bank_details.",
                                                        fromAccountNumber);
                                        BankDetails bankDetails = bankDetailsRepository
                                                        .findByAccountNumber(fromAccountNumber)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                        "Sender account not found with number: "
                                                                                        + fromAccountNumber));

                                        // Provision new account for existing bank user
                                        Account newAccount = Account.builder()
                                                        .accountNumber(fromAccountNumber)
                                                        .holderName(bankDetails.getUserName()) // Use userName from
                                                                                               // BankDetails
                                                        .balance(new BigDecimal("10000.00")) // Default balance for demo
                                                        .status(AccountStatus.ACTIVE)
                                                        .build();

                                        logger.info("Auto-provisioned account for user: {}", bankDetails.getUserName());
                                        return accountRepository.save(newAccount);
                                });

                Optional<Account> toAccountOpt = accountRepository.findByAccountNumber(toAccountNumber);

                BigDecimal fromBalanceBefore = fromAccount.getBalance();

                if (toAccountOpt.isPresent()) {
                        // Internal Transfer
                        Account toAccount = toAccountOpt.get();
                        BigDecimal toBalanceBefore = toAccount.getBalance();

                        fromAccount.debit(amount);
                        toAccount.credit(amount);

                        TransactionLog txLog = TransactionLog.builder()
                                        .fromAccountId(fromAccount.getId())
                                        .toAccountId(toAccount.getId())
                                        .amount(amount)
                                        .type(TransactionType.TRANSFER)
                                        .status(TransactionStatus.SUCCESS)
                                        .description(description != null ? description
                                                        : "Transfer to " + toAccountNumber)
                                        .fromAccountBalanceBefore(fromBalanceBefore)
                                        .fromAccountBalanceAfter(fromAccount.getBalance())
                                        .toAccountBalanceBefore(toBalanceBefore)
                                        .toAccountBalanceAfter(toAccount.getBalance())
                                        .transactionDate(LocalDateTime.now())
                                        .build();

                        TransactionLog savedTx = transactionLogRepository.save(txLog);
                        return convertToDTO(savedTx, fromAccount, toAccount);
                } else {
                        // External Debit Case
                        // 12-Digit Rule Verification
                        if (toAccountNumber == null || !toAccountNumber.matches("\\d{12}")) {
                                throw new IllegalArgumentException(
                                                "Recipient account number must be 12 digits for external transfers.");
                        }

                        logger.info("Recipient {} not found in system. Processing as external debit.", toAccountNumber);

                        // External Debit Only
                        fromAccount.debit(amount);

                        TransactionLog txLog = TransactionLog.builder()
                                        .fromAccountId(fromAccount.getId())
                                        .toAccountId(fromAccount.getId()) // Self reference for external debit
                                        .amount(amount)
                                        .type(TransactionType.DEBIT)
                                        .status(TransactionStatus.SUCCESS)
                                        .description(description != null ? description
                                                        : "External payment to " + toAccountNumber)
                                        .fromAccountBalanceBefore(fromBalanceBefore)
                                        .fromAccountBalanceAfter(fromAccount.getBalance())
                                        .transactionDate(LocalDateTime.now())
                                        .build();

                        TransactionLog savedTx = transactionLogRepository.save(txLog);
                        return convertToDTO(savedTx, fromAccount, null);
                }
        }

        /**
         * Log a credit transaction
         */
        @Transactional
        public TransactionLog logCreditTransaction(Account account, BigDecimal amount, String description) {
                BigDecimal balanceBefore = account.getBalance();

                TransactionLog txLog = TransactionLog.builder()
                                .toAccountId(account.getId())
                                .fromAccountId(account.getId()) // Self reference for deposits
                                .amount(amount)
                                .type(TransactionType.CREDIT)
                                .status(TransactionStatus.SUCCESS)
                                .description(description != null ? description
                                                : "Deposit to account " + account.getAccountNumber())
                                .toAccountBalanceBefore(balanceBefore)
                                .toAccountBalanceAfter(account.getBalance())
                                .transactionDate(LocalDateTime.now())
                                .build();

                return transactionLogRepository.save(txLog);
        }

        /**
         * Log a debit transaction
         */
        @Transactional
        public TransactionLog logDebitTransaction(Account account, BigDecimal amount, String description) {
                BigDecimal balanceBefore = account.getBalance();

                TransactionLog txLog = TransactionLog.builder()
                                .fromAccountId(account.getId())
                                .toAccountId(account.getId()) // Self reference for withdrawals
                                .amount(amount)
                                .type(TransactionType.DEBIT)
                                .status(TransactionStatus.SUCCESS)
                                .description(description != null ? description
                                                : "Withdrawal from account " + account.getAccountNumber())
                                .fromAccountBalanceBefore(balanceBefore)
                                .fromAccountBalanceAfter(account.getBalance())
                                .transactionDate(LocalDateTime.now())
                                .build();

                return transactionLogRepository.save(txLog);
        }

        /**
         * Get transaction history for an account
         */
        public List<TransactionDTO> getAccountTransactionHistory(Long accountId) {
                Account account = accountRepository.findById(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Account not found with ID: " + accountId));

                List<TransactionLog> transactions = transactionLogRepository.findByAccountId(accountId);

                return transactions.stream()
                                .map(tx -> convertToDTO(tx, null, null))
                                .collect(Collectors.toList());
        }

        /**
         * Get all transactions
         */
        public List<TransactionDTO> getAllTransactions() {
                return transactionLogRepository.findAll().stream()
                                .map(tx -> convertToDTO(tx, null, null))
                                .collect(Collectors.toList());
        }

        /**
         * Get transaction by ID
         */
        public TransactionDTO getTransactionById(Long id) {
                TransactionLog txLog = transactionLogRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction not found with ID: " + id));

                return convertToDTO(txLog, null, null);
        }

        /**
         * Get failed transactions for an account
         */
        public List<TransactionDTO> getFailedTransactions(Long accountId) {
                return transactionLogRepository.findFailedTransactionsByAccountId(accountId).stream()
                                .map(tx -> convertToDTO(tx, null, null))
                                .collect(Collectors.toList());
        }

        /**
         * Convert TransactionLog to DTO with account details
         */
        private TransactionDTO convertToDTO(TransactionLog txLog, Account fromAccount, Account toAccount) {
                // Fetch accounts if not provided
                if (fromAccount == null && txLog.getFromAccountId() != null) {
                        fromAccount = accountRepository.findById(txLog.getFromAccountId()).orElse(null);
                }
                if (toAccount == null && txLog.getToAccountId() != null) {
                        toAccount = accountRepository.findById(txLog.getToAccountId()).orElse(null);
                }

                return TransactionDTO.builder()
                                .id(txLog.getId())
                                .fromAccountId(txLog.getFromAccountId())
                                .fromAccountNumber(fromAccount != null ? fromAccount.getAccountNumber() : null)
                                .fromAccountHolderName(fromAccount != null ? fromAccount.getHolderName() : null)
                                .toAccountId(txLog.getToAccountId())
                                .toAccountNumber(toAccount != null ? toAccount.getAccountNumber() : null)
                                .toAccountHolderName(toAccount != null ? toAccount.getHolderName() : null)
                                .amount(txLog.getAmount())
                                .type(txLog.getType())
                                .status(txLog.getStatus())
                                .transactionDate(txLog.getTransactionDate())
                                .description(txLog.getDescription())
                                .fromAccountBalanceBefore(txLog.getFromAccountBalanceBefore())
                                .fromAccountBalanceAfter(txLog.getFromAccountBalanceAfter())
                                .toAccountBalanceBefore(txLog.getToAccountBalanceBefore())
                                .toAccountBalanceAfter(txLog.getToAccountBalanceAfter())
                                .failureReason(txLog.getFailureReason())
                                .build();
        }
}