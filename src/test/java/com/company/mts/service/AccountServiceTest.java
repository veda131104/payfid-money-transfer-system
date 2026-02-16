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
        when(accountRepository.existsByHolderNameIgnoreCase("John Doe")).thenReturn(false);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = accountService.createAccount("John Doe", new BigDecimal("1000.00"));

        assertNotNull(result);
        assertEquals("John Doe", result.getHolderName());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccount_DuplicateHolder_ThrowsException() {
        when(accountRepository.existsByHolderNameIgnoreCase("John Doe")).thenReturn(true);

        assertThrows(DuplicateAccountException.class, () -> {
            accountService.createAccount("John Doe", new BigDecimal("1000.00"));
        });

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_BelowMinimumBalance_ThrowsException() {
        when(accountRepository.existsByHolderNameIgnoreCase("John Doe")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount("John Doe", new BigDecimal("50.00"));
        });
    }

    @Test
    void createAccount_MaxAttemptsReached_ThrowsException() {
        when(accountRepository.existsByHolderNameIgnoreCase(anyString())).thenReturn(false);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            accountService.createAccount("New User", new BigDecimal("1000.00"));
        });
    }

    @Test
    void getAccount_Found() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        Account result = accountService.getAccount(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccount(999L));
    }

    @Test
    void getAccountByAccountNumber_Success() {
        when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(testAccount));
        Account result = accountService.getAccountByAccountNumber("123456789012");
        assertNotNull(result);
        assertEquals("John Doe", result.getHolderName());
    }

    @Test
    void getAccountByHolderName_Success() {
        when(accountRepository.findByHolderNameIgnoreCase("John Doe")).thenReturn(Optional.of(testAccount));
        Account result = accountService.getAccountByHolderName("John Doe");
        assertNotNull(result);
        assertEquals("123456789012", result.getAccountNumber());
    }

    @Test
    void credit_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        Account result = accountService.credit(1L, new BigDecimal("500.00"));
        assertEquals(new BigDecimal("1500.00"), result.getBalance());
    }

    @Test
    void debit_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        Account result = accountService.debit(1L, new BigDecimal("300.00"));
        assertEquals(new BigDecimal("700.00"), result.getBalance());
    }

    @Test
    void debit_InsufficientBalance_ThrowsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        assertThrows(InsufficientBalanceException.class, () -> accountService.debit(1L, new BigDecimal("2000.00")));
    }

    @Test
    void transfer_Success() {
        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("234567890123")
                .holderName("Jane Smith")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        accountService.transfer(1L, 2L, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
    }

    @Test
    void transferByAccountNumber_Success() {
        Account toAccount = Account.builder()
                .accountNumber("234567890123")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("234567890123")).thenReturn(Optional.of(toAccount));

        accountService.transferByAccountNumber("123456789012", "234567890123", new BigDecimal("200.00"));

        assertEquals(new BigDecimal("800.00"), testAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());
    }

    @Test
    void lockAccount_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        Account result = accountService.lockAccount(1L);
        assertEquals(AccountStatus.LOCKED, result.getStatus());
    }

    @Test
    void unlockAccount_Success() {
        testAccount.setStatus(AccountStatus.LOCKED);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        Account result = accountService.unlockAccount(1L);
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
    }

    @Test
    void closeAccount_Success() {
        testAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        Account result = accountService.closeAccount(1L);
        assertEquals(AccountStatus.CLOSED, result.getStatus());
    }
}