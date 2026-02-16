package com.company.mts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class AnalyticsService {

    @Value("${snowflake.url}")
    private String snowflakeUrl;

    @Value("${snowflake.user}")
    private String snowflakeUser;

    @Value("${snowflake.password}")
    private String snowflakePassword;

    @Value("${snowflake.role}")
    private String snowflakeRole;

    @Value("${snowflake.warehouse}")
    private String snowflakeWarehouse;

    @Value("${snowflake.database}")
    private String snowflakeDatabase;

    @Value("${snowflake.schema}")
    private String snowflakeSchema;

    private boolean isSnowflakeConfigured() {
        return snowflakeUser != null && !snowflakeUser.contains("<your-username>");
    }

    protected Connection connect() throws SQLException {
        Properties properties = new Properties();
        properties.put("user", snowflakeUser);
        properties.put("password", snowflakePassword);
        properties.put("role", snowflakeRole);
        properties.put("warehouse", snowflakeWarehouse);
        properties.put("db", snowflakeDatabase);
        properties.put("schema", snowflakeSchema);
        properties.put("jdbc_query_result_format", "json");

        return DriverManager.getConnection(snowflakeUrl, properties);
    }

    public Map<String, Object> checkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", isSnowflakeConfigured());
        status.put("url", snowflakeUrl);
        status.put("user", snowflakeUser);

        try (Connection conn = connect()) {
            status.put("connection", "SUCCESS");
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM STG_TRANSACTIONS")) {
                if (rs.next()) {
                    status.put("rowCount", rs.getInt(1));
                    status.put("tableExists", true);
                }
            } catch (SQLException e) {
                status.put("tableExists", false);
                status.put("error", e.getMessage());
            }
        } catch (SQLException e) {
            status.put("connection", "FAILED");
            status.put("error", e.getMessage());
        }
        return status;
    }

    public List<Map<String, Object>> getTransactionVolume(String name, String role, String holderName) {
        if (!isSnowflakeConfigured()) {
            return getDummyData();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT DATE_TRUNC('DAY', transaction_date) as day, SUM(amount) as volume " +
                "FROM STG_TRANSACTIONS " +
                "WHERE (UPPER(?) = 'ADMIN' OR UPPER(sender_name) = UPPER(?)) " +
                "GROUP BY 1 ORDER BY 1 ASC";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setString(2, holderName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", rs.getDate("DAY").toString());
                    row.put("value", rs.getBigDecimal("VOLUME"));
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("Error querying Snowflake analytics: {}", e.getMessage());
        }
        return results;
    }

    public List<Map<String, Object>> getAccountActivity(String name, String role, String holderName) {
        if (!isSnowflakeConfigured()) {
            return getDummyAccountActivity();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String sql;
        if (role.equalsIgnoreCase("ADMIN")) {
            sql = "SELECT sender_name as name, COUNT(*) as txn_count " +
                    "FROM STG_TRANSACTIONS " +
                    "WHERE sender_name IS NOT NULL " +
                    "GROUP BY 1 ORDER BY 2 DESC LIMIT 5";
        } else {
            // For regular users, show top people they sent money to OR received from
            // (excluding themselves)
            sql = "SELECT counterpart, COUNT(*) as txn_count FROM (" +
                    "  SELECT receiver_name as counterpart FROM STG_TRANSACTIONS WHERE UPPER(sender_name) = UPPER(?) AND UPPER(receiver_name) != UPPER(?) "
                    +
                    "  UNION ALL " +
                    "  SELECT sender_name as counterpart FROM STG_TRANSACTIONS WHERE UPPER(receiver_name) = UPPER(?) AND UPPER(sender_name) != UPPER(?) "
                    +
                    ") GROUP BY 1 ORDER BY 2 DESC LIMIT 5";
        }

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (role.equalsIgnoreCase("ADMIN")) {
                // No params needed
            } else {
                pstmt.setString(1, holderName);
                pstmt.setString(2, holderName);
                pstmt.setString(3, holderName);
                pstmt.setString(4, holderName);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", rs.getString(1)); // First column is the name
                    row.put("count", rs.getInt(2)); // Second column is the count
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("Error querying Snowflake account activity: {}", e.getMessage());
        }
        return results;
    }

    public Map<String, Object> getSuccessRate(String name, String role, String holderName) {
        if (!isSnowflakeConfigured()) {
            return getDummySuccessRate();
        }

        Map<String, Object> results = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as count FROM STG_TRANSACTIONS " +
                "WHERE (UPPER(?) = 'ADMIN' OR UPPER(sender_name) = UPPER(?)) " +
                "GROUP BY 1";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setString(2, holderName);

            try (ResultSet rs = pstmt.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String status = rs.getString("STATUS");
                    int count = rs.getInt("COUNT");
                    results.put(status.toLowerCase(), count);
                    total += count;
                }

            }
        } catch (SQLException e) {
            log.error("Error querying Snowflake success rate: {}", e.getMessage());
        }
        return results;
    }

    public List<Map<String, Object>> getPeakHours(String name, String role, String holderName) {
        if (!isSnowflakeConfigured()) {
            return getDummyPeakHours();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT EXTRACT(HOUR FROM transaction_date) as hour, COUNT(*) as count " +
                "FROM STG_TRANSACTIONS " +
                "WHERE (UPPER(?) = 'ADMIN' OR UPPER(sender_name) = UPPER(?)) " +
                "GROUP BY 1 ORDER BY 1 ASC";

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setString(2, holderName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("hour", rs.getInt("HOUR") + "h");
                    row.put("count", rs.getInt("COUNT"));
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("Error querying Snowflake peak hours: {}", e.getMessage());
        }
        return results;
    }

    private List<Map<String, Object>> getDummyData() {
        List<Map<String, Object>> dummy = new ArrayList<>();
        String[] days = { "2026-02-10", "2026-02-11", "2026-02-12", "2026-02-13", "2026-02-14", "2026-02-15" };
        double[] volumes = { 1200.50, 4500.00, 3200.75, 5800.20, 2100.10, 8900.45 };

        for (int i = 0; i < days.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", days[i]);
            row.put("value", volumes[i]);
            dummy.add(row);
        }
        return dummy;
    }

    private Map<String, Object> getDummySuccessRate() {
        Map<String, Object> dummy = new HashMap<>();
        dummy.put("success", 85);
        dummy.put("failed", 10);
        dummy.put("pending", 5);
        return dummy;
    }

    private List<Map<String, Object>> getDummyAccountActivity() {
        List<Map<String, Object>> dummy = new ArrayList<>();
        String[] names = { "Veda Jagannath", "Samyuktha J.", "Company A", "Global Payouts", "User 123" };
        int[] counts = { 45, 38, 25, 22, 18 };
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", names[i]);
            row.put("count", counts[i]);
            dummy.add(row);
        }
        return dummy;
    }

    private List<Map<String, Object>> getDummyPeakHours() {
        List<Map<String, Object>> dummy = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("hour", i + "h");
            row.put("count", Math.floor(Math.random() * 50) + 10);
            dummy.add(row);
        }
        return dummy;
    }
}
