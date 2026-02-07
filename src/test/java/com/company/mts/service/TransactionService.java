package com.company.mts.service;

import com.company.mts.dto.TransactionDTO;
import com.company.mts.entity.*;
import com.company.mts.exception.DuplicateTransactionException;
import com.company.mts.exception.InsufficientBalanceException;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private TransactionLog existingTransaction;

    @BeforeEach
    void setUp() {
        fromAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .holderName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        toAccount = Account.builder()
                .id(2L)
                .accountNumber("2345678901")
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
        // Arrange
        String idempotencyKey = "TEST-KEY-002";
        BigDecimal amount = new BigDecimal("300.00");

        when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> {
                    TransactionLog tx = invocation.getArgument(0);
                    tx.setId(2L);
                    return tx;
                });

        // Act
        TransactionDTO result = transactionService.executeIdempotentTransfer(
                1L, 2L, amount, idempotencyKey, "Test transfer"
        );

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("700.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    void executeIdempotentTransfer_DuplicateKey_ThrowsException() {
        // Arrange
        String idempotencyKey = "TEST-KEY-001";
        BigDecimal amount = new BigDecimal("300.00");

        when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);
        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTransaction));

        // Act & Assert
        DuplicateTransactionException exception = assertThrows(
                DuplicateTransactionException.class,
                () -> transactionService.executeIdempotentTransfer(
                        1L, 2L, amount, idempotencyKey, "Test transfer"
                )
        );

        assertEquals(1L, exception.getExistingTransactionId());
        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
    }

    @Test
    void executeIdempotentTransfer_InsufficientBalance_LogsFailure() {
        // Arrange
        String idempotencyKey = "TEST-KEY-003";
        BigDecimal amount = new BigDecimal("2000.00"); // More than balance

        when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            transactionService.executeIdempotentTransfer(
                    1L, 2L, amount, idempotencyKey, "Test transfer"
            );
        });

        // Verify that failed transaction was logged
        verify(transactionLogRepository, times(1)).save(
                argThat(tx -> tx.getStatus() == TransactionStatus.FAILED)
        );
    }

    @Test
    void logCreditTransaction_Success() {
        // Arrange
        BigDecimal amount = new BigDecimal("500.00");
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionLog result = transactionService.logCreditTransaction(
                fromAccount, amount, "Deposit"
        );

        // Assert
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(amount, result.getAmount());
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    void logDebitTransaction_Success() {
        // Arrange
        BigDecimal amount = new BigDecimal("200.00");
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionLog result = transactionService.logDebitTransaction(
                fromAccount, amount, "Withdrawal"
        );

        // Assert
        assertNotNull(result);
        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(amount, result.getAmount());
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }
}