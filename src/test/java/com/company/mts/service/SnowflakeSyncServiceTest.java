package com.company.mts.service;

import com.company.mts.entity.Account;
import com.company.mts.entity.TransactionLog;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnowflakeSyncServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @Spy
    @InjectMocks
    private SnowflakeSyncService snowflakeSyncService;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        ReflectionTestUtils.setField(snowflakeSyncService, "snowflakeUser", "test-user");
        lenient().doReturn(mockConnection).when(snowflakeSyncService).connect();
    }

    @Test
    void syncToSnowflake_NoNewTransactions() throws SQLException {
        // Arrange
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(100L); // Max ID in Snowflake

        when(transactionLogRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        snowflakeSyncService.syncToSnowflake();

        // Assert
        verify(transactionLogRepository).findAll();
        verify(mockPreparedStatement, never()).executeBatch();
    }

    @Test
    void syncToSnowflake_WithNewTransactions_Success() throws SQLException {
        // Arrange
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(0L);

        TransactionLog tx = TransactionLog.builder()
                .id(1L)
                .fromAccountId(10L)
                .toAccountId(20L)
                .amount(new BigDecimal("500.00"))
                .build();

        Account fromAcc = Account.builder().id(10L).holderName("Sender").build();
        Account toAcc = Account.builder().id(20L).holderName("Receiver").build();

        when(transactionLogRepository.findAll()).thenReturn(List.of(tx));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAcc));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(toAcc));
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        snowflakeSyncService.syncToSnowflake();

        // Assert
        verify(mockPreparedStatement).addBatch();
        verify(mockPreparedStatement).executeBatch();
        verify(accountRepository, times(2)).findById(anyLong());
    }

    @Test
    void isSnowflakeConfigured_False() {
        // Arrange
        ReflectionTestUtils.setField(snowflakeSyncService, "snowflakeUser", "<your-username>");

        // Act & Assert
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(snowflakeSyncService, "isSnowflakeConfigured"));
    }
}
