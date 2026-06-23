package com.company.mts.entity;

import com.company.mts.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void debit_WithInsufficientBalance_ShouldThrowException() {
        // Arrange: account with small balance
        Account account = Account.builder()
                .balance(new BigDecimal("50.00"))
                .status(AccountStatus.ACTIVE)
                .build();
        // Act & Assert
        InsufficientBalanceException ex = assertThrows(
                InsufficientBalanceException.class,
                () -> account.debit(new BigDecimal("100.00"))
        );
        assertTrue(ex.getMessage().contains("Insufficient balance"));
    }

    @Test
    void debit_WithSufficientBalance_ShouldUpdateBalance() {
        // Arrange
        Account account = Account.builder()
                .balance(new BigDecimal("200.00"))
                .status(AccountStatus.ACTIVE)
                .build();
        // Act
        account.debit(new BigDecimal("75.00"));
        // Assert
        assertEquals(new BigDecimal("125.00"), account.getBalance());
    }
}
