package com.company.mts.service;

import com.company.mts.entity.Account;
import com.company.mts.entity.AccountStatus;
import com.company.mts.exception.DuplicateAccountException;
import com.company.mts.exception.InsufficientBalanceException;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.AccountRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .accountNumber("123456789012")
                .holderName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void createAccount_Success() {
        // Arrange
        when(accountRepository.existsByHolderNameIgnoreCase("John Doe")).thenReturn(false);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // Act
        Account result = accountService.createAccount("John Doe", new BigDecimal("1000.00"));

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getHolderName());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccount_DuplicateHolder_ThrowsException() {
        // Arrange
        when(accountRepository.existsByHolderNameIgnoreCase("John Doe")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateAccountException.class, () -> {
            accountService.createAccount("John Doe", new BigDecimal("1000.00"));
        });

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_BelowMinimumBalance_ThrowsException() {
        // Arrange
        when(accountRepository.existsByHolderNameIgnoreCase("John Doe")).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount("John Doe", new BigDecimal("50.00"));
        });

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccount_Found() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        Account result = accountService.getAccount(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getHolderName());
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            accountService.getAccount(999L);
        });
    }

    @Test
    void credit_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        BigDecimal creditAmount = new BigDecimal("500.00");

        // Act
        Account result = accountService.credit(1L, creditAmount);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
    }

    @Test
    void debit_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        BigDecimal debitAmount = new BigDecimal("300.00");

        // Act
        Account result = accountService.debit(1L, debitAmount);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("700.00"), result.getBalance());
    }

    @Test
    void debit_InsufficientBalance_ThrowsException() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        BigDecimal debitAmount = new BigDecimal("2000.00");

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            accountService.debit(1L, debitAmount);
        });
    }

    @Test
    void transfer_Success() {
        // Arrange
        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("234567890123")
                .holderName("Jane Smith")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        BigDecimal transferAmount = new BigDecimal("300.00");

        // Act
        accountService.transfer(1L, 2L, transferAmount);

        // Assert
        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
    }

    @Test
    void transfer_SameAccount_ThrowsException() {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("100.00");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer(1L, 1L, transferAmount);
        });
    }

    @Test
    void lockAccount_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        Account result = accountService.lockAccount(1L);

        // Assert
        assertEquals(AccountStatus.LOCKED, result.getStatus());
    }

    @Test
    void closeAccount_WithZeroBalance_Success() {
        // Arrange
        testAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act
        Account result = accountService.closeAccount(1L);

        // Assert
        assertEquals(AccountStatus.CLOSED, result.getStatus());
    }

    @Test
    void closeAccount_WithNonZeroBalance_ThrowsException() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            accountService.closeAccount(1L);
        });
    }
}