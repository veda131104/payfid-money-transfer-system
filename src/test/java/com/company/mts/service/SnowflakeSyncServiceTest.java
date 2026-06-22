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

    @Test
    void syncToSnowflake_Unconfigured_Skips() {
        ReflectionTestUtils.setField(snowflakeSyncService, "snowflakeUser", "<your-username>");
        snowflakeSyncService.syncToSnowflake();
        verify(transactionLogRepository, never()).findAll();
    }

    @Test
    void syncToSnowflake_MaxQueryThrowsSQLException_Continues() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException("Query fail"));
        when(transactionLogRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> snowflakeSyncService.syncToSnowflake());
    }

    @Test
    void syncToSnowflake_AlterTableThrowsSQLException_Continues() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(0L);

        TransactionLog tx = TransactionLog.builder().id(1L).build();
        when(transactionLogRepository.findAll()).thenReturn(List.of(tx));

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        doThrow(new SQLException("Alter fail")).when(mockStatement).execute(anyString());

        assertDoesNotThrow(() -> snowflakeSyncService.syncToSnowflake());
    }

    @Test
    void syncToSnowflake_SyncException_LogsError() throws SQLException {
        doThrow(new SQLException("Conn fail")).when(snowflakeSyncService).connect();
        assertDoesNotThrow(() -> snowflakeSyncService.syncToSnowflake());
    }

    @Test
    void syncToSnowflake_VariousTransactionScenarios() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(0L);

        // Scenario A: fromAccountId null
        TransactionLog tx1 = TransactionLog.builder().id(1L).build();

        // Scenario B: fromAccountId not found
        TransactionLog tx2 = TransactionLog.builder().id(2L).fromAccountId(10L).build();
        when(accountRepository.findById(10L)).thenReturn(Optional.empty());

        // Scenario C: toAccountId null
        TransactionLog tx3 = TransactionLog.builder().id(3L).fromAccountId(20L).build();
        Account fromAcc = Account.builder().id(20L).holderName("Sender").build();
        when(accountRepository.findById(20L)).thenReturn(Optional.of(fromAcc));

        // Scenario D: toAccountId equals fromAccountId, description contains "to "
        TransactionLog tx4 = TransactionLog.builder().id(4L).fromAccountId(20L).toAccountId(20L).description("Transfer to ReceiverAccount").build();

        // Scenario E: toAccountId equals fromAccountId, description contains "Withdrawal"
        TransactionLog tx5 = TransactionLog.builder().id(5L).fromAccountId(20L).toAccountId(20L).description("Withdrawal from ATM").build();

        // Scenario F: toAccountId equals fromAccountId, description contains "Deposit"
        TransactionLog tx6 = TransactionLog.builder().id(6L).fromAccountId(20L).toAccountId(20L).description("Deposit money").build();

        // Scenario G: toAccountId equals fromAccountId, description other
        TransactionLog tx7 = TransactionLog.builder().id(7L).fromAccountId(20L).toAccountId(20L).description("Payment").build();

        // Scenario H: toAccountId not found
        TransactionLog tx8 = TransactionLog.builder().id(8L).fromAccountId(20L).toAccountId(30L).build();
        when(accountRepository.findById(30L)).thenReturn(Optional.empty());

        List<TransactionLog> transactions = List.of(tx1, tx2, tx3, tx4, tx5, tx6, tx7, tx8);
        when(transactionLogRepository.findAll()).thenReturn(transactions);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        snowflakeSyncService.syncToSnowflake();

        verify(mockPreparedStatement, times(8)).addBatch();
    }

    @Test
    void realConnect_ThrowsSQLException() {
        SnowflakeSyncService service = new SnowflakeSyncService(transactionLogRepository, accountRepository);
        ReflectionTestUtils.setField(service, "snowflakeUrl", "jdbc:invalid:url");
        ReflectionTestUtils.setField(service, "snowflakeUser", "test");
        ReflectionTestUtils.setField(service, "snowflakePassword", "test");
        ReflectionTestUtils.setField(service, "snowflakeRole", "test");
        ReflectionTestUtils.setField(service, "snowflakeWarehouse", "test");
        ReflectionTestUtils.setField(service, "snowflakeDatabase", "test");
        ReflectionTestUtils.setField(service, "snowflakeSchema", "test");
        assertThrows(SQLException.class, () -> service.connect());
    }
}
