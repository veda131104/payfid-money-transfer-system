package com.company.mts.service;

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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Spy
    @InjectMocks
    private AnalyticsService analyticsService;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        // Set Snowflake properties via reflection since they are @Value annotated
        ReflectionTestUtils.setField(analyticsService, "snowflakeUrl", "jdbc:snowflake://test.url");
        ReflectionTestUtils.setField(analyticsService, "snowflakeUser", "test-user");
        ReflectionTestUtils.setField(analyticsService, "snowflakePassword", "test-password");
        ReflectionTestUtils.setField(analyticsService, "snowflakeRole", "test-role");
        ReflectionTestUtils.setField(analyticsService, "snowflakeWarehouse", "test-warehouse");
        ReflectionTestUtils.setField(analyticsService, "snowflakeDatabase", "test-db");
        ReflectionTestUtils.setField(analyticsService, "snowflakeSchema", "test-schema");

        // Mock the connect() method to return our mock connection leniently
        lenient().doReturn(mockConnection).when(analyticsService).connect();

        // Stub createStatement leniently to prevent NullPointerException
        Statement mockStmt = mock(Statement.class);
        lenient().when(mockConnection.createStatement()).thenReturn(mockStmt);
        lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        ResultSet mockRs = mock(ResultSet.class);
        lenient().when(mockStmt.executeQuery(anyString())).thenReturn(mockRs);
    }

    @Test
    void getTransactionVolume_Admin_Success() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getDate("DAY")).thenReturn(Date.valueOf("2026-02-15"));
        when(mockResultSet.getBigDecimal("VOLUME")).thenReturn(new BigDecimal("5000.00"));

        // Act
        List<Map<String, Object>> results = analyticsService.getTransactionVolume("veda", "ADMIN", "Veda Jagannath");

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("2026-02-15", results.get(0).get("date"));
        assertEquals(new BigDecimal("5000.00"), results.get(0).get("value"));
        verify(mockPreparedStatement).setString(1, "ADMIN");
    }

    @Test
    void getAccountActivity_User_Success() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString(1)).thenReturn("Counterparty A");
        when(mockResultSet.getInt(2)).thenReturn(10);

        // Act
        List<Map<String, Object>> results = analyticsService.getAccountActivity("janani", "USER", "Janani");

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("Counterparty A", results.get(0).get("name"));
        assertEquals(10, results.get(0).get("count"));
        verify(mockPreparedStatement).setString(1, "Janani");
    }

    @Test
    void getSuccessRate_Success() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("STATUS")).thenReturn("SUCCESS", "FAILED");
        when(mockResultSet.getInt("COUNT")).thenReturn(80, 20);

        // Act
        Map<String, Object> results = analyticsService.getSuccessRate("veda", "ADMIN", "Veda Jagannath");

        // Assert
        assertNotNull(results);
        assertEquals(80, results.get("success"));
        assertEquals(20, results.get("failed"));
    }

    @Test
    void getPeakHours_Success() throws SQLException {
        // Arrange
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("HOUR")).thenReturn(14);
        when(mockResultSet.getInt("COUNT")).thenReturn(150);

        // Act
        List<Map<String, Object>> results = analyticsService.getPeakHours("veda", "ADMIN", "Veda Jagannath");

        // Assert
        assertNotNull(results);
        assertEquals("14h", results.get(0).get("hour"));
        assertEquals(150, results.get(0).get("count"));
    }

    @Test
    void isSnowflakeConfigured_False_ReturnsDummy() {
        ReflectionTestUtils.setField(analyticsService, "snowflakeUser", "<your-username>");

        List<Map<String, Object>> vol = analyticsService.getTransactionVolume("user", "USER", "user");
        assertNotNull(vol);
        assertEquals(6, vol.size());

        List<Map<String, Object>> act = analyticsService.getAccountActivity("user", "USER", "user");
        assertNotNull(act);
        assertEquals(5, act.size());

        Map<String, Object> rate = analyticsService.getSuccessRate("user", "USER", "user");
        assertNotNull(rate);
        assertEquals(85, rate.get("success"));

        List<Map<String, Object>> peak = analyticsService.getPeakHours("user", "USER", "user");
        assertNotNull(peak);
        assertEquals(24, peak.size());
    }

    @Test
    void getAccountActivity_Admin_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString(1)).thenReturn("Counterparty Admin");
        when(mockResultSet.getInt(2)).thenReturn(50);

        List<Map<String, Object>> results = analyticsService.getAccountActivity("veda", "ADMIN", "Veda Jagannath");

        assertNotNull(results);
        assertEquals("Counterparty Admin", results.get(0).get("name"));
        assertEquals(50, results.get(0).get("count"));
    }

    @Test
    void checkStatus_ConfiguredFalse() {
        ReflectionTestUtils.setField(analyticsService, "snowflakeUser", "<your-username>");
        Map<String, Object> status = analyticsService.checkStatus();
        assertFalse((Boolean) status.get("configured"));
    }

    @Test
    void checkStatus_ConnectionSuccess() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStmt);
        when(mockStmt.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(42);

        Map<String, Object> status = analyticsService.checkStatus();

        assertEquals("SUCCESS", status.get("connection"));
        assertEquals(42, status.get("rowCount"));
        assertTrue((Boolean) status.get("tableExists"));
    }

    @Test
    void checkStatus_ConnectionFailed() throws SQLException {
        doThrow(new SQLException("Conn fail")).when(analyticsService).connect();

        Map<String, Object> status = analyticsService.checkStatus();

        assertEquals("FAILED", status.get("connection"));
        assertEquals("Conn fail", status.get("error"));
    }

    @Test
    void checkStatus_QueryFailed() throws SQLException {
        Statement mockStmt = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStmt);
        when(mockStmt.executeQuery(anyString())).thenThrow(new SQLException("Query fail"));

        Map<String, Object> status = analyticsService.checkStatus();

        assertEquals("SUCCESS", status.get("connection"));
        assertFalse((Boolean) status.get("tableExists"));
        assertEquals("Query fail", status.get("error"));
    }

    @Test
    void getTransactionVolume_SQLException_Handled() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB fail"));
        List<Map<String, Object>> results = analyticsService.getTransactionVolume("veda", "ADMIN", "Veda Jagannath");
        assertTrue(results.isEmpty());
    }

    @Test
    void getAccountActivity_SQLException_Handled() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB fail"));
        List<Map<String, Object>> results = analyticsService.getAccountActivity("veda", "ADMIN", "Veda Jagannath");
        assertTrue(results.isEmpty());
    }

    @Test
    void getSuccessRate_SQLException_Handled() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB fail"));
        Map<String, Object> results = analyticsService.getSuccessRate("veda", "ADMIN", "Veda Jagannath");
        assertTrue(results.isEmpty());
    }

    @Test
    void getPeakHours_SQLException_Handled() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB fail"));
        List<Map<String, Object>> results = analyticsService.getPeakHours("veda", "ADMIN", "Veda Jagannath");
        assertTrue(results.isEmpty());
    }

    @Test
    void realConnect_ThrowsSQLException() {
        AnalyticsService service = new AnalyticsService();
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
