package com.company.mts.service;

import com.company.mts.entity.TransactionLog;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class SnowflakeSyncService {

    private final TransactionLogRepository transactionLogRepository;
    private final AccountRepository accountRepository;

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

    private Long lastSyncedId = 0L;

    public SnowflakeSyncService(TransactionLogRepository transactionLogRepository,
            AccountRepository accountRepository) {
        this.transactionLogRepository = transactionLogRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Sync data from H2 to Snowflake every 5 minutes.
     * For demo purposes, we'll log the connection attempt and simulated sync if no
     * credentials.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void syncToSnowflake() {
        if (!isSnowflakeConfigured()) {
            log.warn("Snowflake credentials not configured. Skipping sync.");
            return;
        }

        log.info("Starting H2 to Snowflake synchronization...");

        try (Connection conn = connect()) {
            // Initialize lastSyncedId from Snowflake if it's 0 (meaning we just started)
            if (lastSyncedId == 0L) {
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM STG_TRANSACTIONS")) {
                    if (rs.next()) {
                        lastSyncedId = rs.getLong(1);
                        log.info("Initialized lastSyncedId from Snowflake: {}", lastSyncedId);
                    }
                } catch (SQLException e) {
                    log.warn("Could not fetch max ID from Snowflake (table might be empty): {}", e.getMessage());
                }
            }

            List<TransactionLog> pendingTransactions = transactionLogRepository.findAll().stream()
                    .filter(tx -> tx.getId() > lastSyncedId)
                    .toList();

            if (pendingTransactions.isEmpty()) {
                log.info("No new transactions to sync.");
                return;
            }

            log.info("Found {} pending transactions for Snowflake sync.", pendingTransactions.size());
            syncTransactions(conn, pendingTransactions);
            lastSyncedId = pendingTransactions.get(pendingTransactions.size() - 1).getId();
            log.info("Successfully synced {} transactions to Snowflake. Last Synced ID: {}",
                    pendingTransactions.size(), lastSyncedId);
        } catch (SQLException e) {
            log.error("Failed to sync data to Snowflake: {}", e.getMessage());
        }
    }

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

        return DriverManager.getConnection(snowflakeUrl, properties);
    }

    private void syncTransactions(Connection conn, List<TransactionLog> transactions) throws SQLException {
        // Ensure column exists (one-time check/alter)
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE STG_TRANSACTIONS ADD COLUMN IF NOT EXISTS sender_name STRING");
            stmt.execute("ALTER TABLE STG_TRANSACTIONS ADD COLUMN IF NOT EXISTS receiver_name STRING");
        } catch (SQLException e) {
            log.warn("Could not alter table: {}", e.getMessage());
        }

        String sql = "INSERT INTO STG_TRANSACTIONS (id, from_account_id, to_account_id, amount, type, status, transaction_date, sender_name, receiver_name) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (TransactionLog tx : transactions) {
                String senderName = "System";
                if (tx.getFromAccountId() != null) {
                    senderName = accountRepository.findById(tx.getFromAccountId())
                            .map(acc -> acc.getHolderName())
                            .orElse("Deleted User");
                }

                String receiverName = "External Entity";
                if (tx.getToAccountId() != null) {
                    // Check if it's an internal transfer
                    if (tx.getToAccountId().equals(tx.getFromAccountId()) && tx.getDescription() != null) {
                        // Extract recipient from description if possible (e.g. "Transfer to
                        // 123456789012")
                        if (tx.getDescription().contains("to ")) {
                            receiverName = tx.getDescription().substring(tx.getDescription().lastIndexOf("to ") + 3);
                        } else if (tx.getDescription().contains("Withdrawal")) {
                            receiverName = "ATM/Cash";
                        } else if (tx.getDescription().contains("Deposit")) {
                            receiverName = senderName; // Self
                            senderName = "Cash Deposit";
                        }
                    } else {
                        receiverName = accountRepository.findById(tx.getToAccountId())
                                .map(acc -> acc.getHolderName())
                                .orElse("Deleted User");
                    }
                }

                pstmt.setLong(1, tx.getId());
                pstmt.setObject(2, tx.getFromAccountId());
                pstmt.setObject(3, tx.getToAccountId());
                pstmt.setBigDecimal(4, tx.getAmount());
                pstmt.setString(5, tx.getType() != null ? tx.getType().name() : null);
                pstmt.setString(6, tx.getStatus() != null ? tx.getStatus().name() : null);
                pstmt.setTimestamp(7,
                        tx.getTransactionDate() != null ? Timestamp.valueOf(tx.getTransactionDate()) : null);
                pstmt.setString(8, senderName);
                pstmt.setString(9, receiverName);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
