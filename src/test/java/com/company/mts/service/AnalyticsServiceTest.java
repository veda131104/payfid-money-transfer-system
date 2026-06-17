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

        // Mock the connect() method to return our mock connection
        doReturn(mockConnection).when(analyticsService).connect();
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
}
