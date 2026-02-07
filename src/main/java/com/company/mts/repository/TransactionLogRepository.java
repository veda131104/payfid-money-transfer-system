package com.company.mts.repository;

import com.company.mts.entity.TransactionLog;
import com.company.mts.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    /**
     * Find transaction by idempotency key
     */
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if transaction exists with idempotency key
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Get all transactions for an account (sent or received)
     */
    @Query("SELECT t FROM TransactionLog t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId ORDER BY t.transactionDate DESC")
    List<TransactionLog> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Get all transactions sent from an account
     */
    List<TransactionLog> findByFromAccountIdOrderByTransactionDateDesc(Long fromAccountId);

    /**
     * Get all transactions received by an account
     */
    List<TransactionLog> findByToAccountIdOrderByTransactionDateDesc(Long toAccountId);

    /**
     * Get transactions between two accounts
     */
    @Query("SELECT t FROM TransactionLog t WHERE " +
            "(t.fromAccountId = :accountId1 AND t.toAccountId = :accountId2) OR " +
            "(t.fromAccountId = :accountId2 AND t.toAccountId = :accountId1) " +
            "ORDER BY t.transactionDate DESC")
    List<TransactionLog> findTransactionsBetweenAccounts(
            @Param("accountId1") Long accountId1,
            @Param("accountId2") Long accountId2);

    /**
     * Get transactions by status
     */
    List<TransactionLog> findByStatusOrderByTransactionDateDesc(TransactionStatus status);

    /**
     * Get transactions within date range
     */
    @Query("SELECT t FROM TransactionLog t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<TransactionLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get failed transactions for an account
     */
    @Query("SELECT t FROM TransactionLog t WHERE " +
            "(t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
            "AND t.status = 'FAILED' ORDER BY t.transactionDate DESC")
    List<TransactionLog> findFailedTransactionsByAccountId(@Param("accountId") Long accountId);
}