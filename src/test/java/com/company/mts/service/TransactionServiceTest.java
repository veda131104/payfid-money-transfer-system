package com.company.mts.service;

import com.company.mts.dto.TransactionDTO;
import com.company.mts.entity.*;
import com.company.mts.exception.DuplicateTransactionException;
import com.company.mts.exception.InsufficientBalanceException;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.BankDetailsRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

        @Mock
        private TransactionLogRepository transactionLogRepository;

        @Mock
        private AccountRepository accountRepository;

        @Mock
        private BankDetailsRepository bankDetailsRepository;

        @InjectMocks
        private TransactionService transactionService;

        private Account fromAccount;
        private Account toAccount;
        private TransactionLog existingTransaction;

        @BeforeEach
        void setUp() {
                fromAccount = Account.builder()
                                .id(1L)
                                .accountNumber("123456789012")
                                .holderName("John Doe")
                                .balance(new BigDecimal("1000.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                toAccount = Account.builder()
                                .id(2L)
                                .accountNumber("234567890123")
                                .holderName("Jane Smith")
                                .balance(new BigDecimal("500.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                existingTransaction = TransactionLog.builder()
                                .id(1L)
                                .fromAccountId(1L)
                                .toAccountId(2L)
                                .amount(new BigDecimal("300.00"))
                                .type(TransactionType.TRANSFER)
                                .status(TransactionStatus.SUCCESS)
                                .idempotencyKey("TEST-KEY-001")
                                .build();
        }

        @Test
        void executeIdempotentTransfer_Success() {
                String idempotencyKey = "TEST-KEY-002";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                TransactionDTO result = transactionService.executeIdempotentTransfer(
                                1L, 2L, new BigDecimal("300.00"), idempotencyKey, "Test transfer");

                assertNotNull(result);
                assertEquals(new BigDecimal("700.00"), fromAccount.getBalance());
                assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
        }

        @Test
        void executeIdempotentTransfer_DuplicateKey_ThrowsException() {
                String idempotencyKey = "TEST-KEY-001";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);
                when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                                .thenReturn(Optional.of(existingTransaction));

                assertThrows(DuplicateTransactionException.class, () -> transactionService.executeIdempotentTransfer(1L,
                                2L, new BigDecimal("300.00"), idempotencyKey, "Test"));
        }

        @Test
        void executeTransferByAccountNumber_Internal_Success() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber("234567890123")).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                TransactionDTO result = transactionService.executeTransferByAccountNumber(
                                "123456789012", "234567890123", new BigDecimal("100.00"), "Internal test");

                assertNotNull(result);
                assertEquals(new BigDecimal("900.00"), fromAccount.getBalance());
        }

        @Test
        void executeTransferByAccountNumber_External_Success() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber("987654321098")).thenReturn(Optional.empty());
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                TransactionDTO result = transactionService.executeTransferByAccountNumber(
                                "123456789012", "987654321098", new BigDecimal("100.00"), "External test");

                assertNotNull(result);
                assertEquals(TransactionType.DEBIT, result.getType());
        }

        @Test
        void logCreditTransaction_Success() {
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));
                TransactionLog result = transactionService.logCreditTransaction(fromAccount, new BigDecimal("500.00"),
                                "Deposit");
                assertNotNull(result);
                assertEquals(TransactionType.CREDIT, result.getType());
        }

        @Test
        void getAccountTransactionHistory_Success() {
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(transactionLogRepository.findByAccountId(1L)).thenReturn(List.of(existingTransaction));

                List<TransactionDTO> results = transactionService.getAccountTransactionHistory(1L);
                assertEquals(1, results.size());
        }

        @Test
        void getTransactionById_Success() {
                when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(existingTransaction));
                TransactionDTO result = transactionService.getTransactionById(1L);
                assertNotNull(result);
                assertEquals(1L, result.getId());
        }
}