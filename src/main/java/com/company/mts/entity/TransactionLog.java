package com.company.mts.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs", indexes = {
        @Index(name = "idx_from_account", columnList = "fromAccountId"),
        @Index(name = "idx_to_account", columnList = "toAccountId"),
        @Index(name = "idx_transaction_date", columnList = "transactionDate"),
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey")
})
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String description;

    // Balance snapshots for audit trail
    @Column(precision = 19, scale = 2)
    private BigDecimal fromAccountBalanceBefore;

    @Column(precision = 19, scale = 2)
    private BigDecimal fromAccountBalanceAfter;

    @Column(precision = 19, scale = 2)
    private BigDecimal toAccountBalanceBefore;

    @Column(precision = 19, scale = 2)
    private BigDecimal toAccountBalanceAfter;

    // Idempotency key for preventing duplicate transactions
    @Column(unique = true, length = 100)
    private String idempotencyKey;

    @Column(length = 1000)
    private String failureReason;

    // Constructors
    public TransactionLog() {
    }

    private TransactionLog(Builder builder) {
        this.id = builder.id;
        this.fromAccountId = builder.fromAccountId;
        this.toAccountId = builder.toAccountId;
        this.amount = builder.amount;
        this.type = builder.type;
        this.status = builder.status;
        this.transactionDate = builder.transactionDate;
        this.description = builder.description;
        this.fromAccountBalanceBefore = builder.fromAccountBalanceBefore;
        this.fromAccountBalanceAfter = builder.fromAccountBalanceAfter;
        this.toAccountBalanceBefore = builder.toAccountBalanceBefore;
        this.toAccountBalanceAfter = builder.toAccountBalanceAfter;
        this.idempotencyKey = builder.idempotencyKey;
        this.failureReason = builder.failureReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long fromAccountId;
        private Long toAccountId;
        private BigDecimal amount;
        private TransactionType type;
        private TransactionStatus status;
        private LocalDateTime transactionDate;
        private String description;
        private BigDecimal fromAccountBalanceBefore;
        private BigDecimal fromAccountBalanceAfter;
        private BigDecimal toAccountBalanceBefore;
        private BigDecimal toAccountBalanceAfter;
        private String idempotencyKey;
        private String failureReason;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder fromAccountId(Long fromAccountId) {
            this.fromAccountId = fromAccountId;
            return this;
        }

        public Builder toAccountId(Long toAccountId) {
            this.toAccountId = toAccountId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder transactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder fromAccountBalanceBefore(BigDecimal fromAccountBalanceBefore) {
            this.fromAccountBalanceBefore = fromAccountBalanceBefore;
            return this;
        }

        public Builder fromAccountBalanceAfter(BigDecimal fromAccountBalanceAfter) {
            this.fromAccountBalanceAfter = fromAccountBalanceAfter;
            return this;
        }

        public Builder toAccountBalanceBefore(BigDecimal toAccountBalanceBefore) {
            this.toAccountBalanceBefore = toAccountBalanceBefore;
            return this;
        }

        public Builder toAccountBalanceAfter(BigDecimal toAccountBalanceAfter) {
            this.toAccountBalanceAfter = toAccountBalanceAfter;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public TransactionLog build() {
            return new TransactionLog(this);
        }
    }

    @PrePersist
    public void prePersist() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getFromAccountBalanceBefore() {
        return fromAccountBalanceBefore;
    }

    public void setFromAccountBalanceBefore(BigDecimal fromAccountBalanceBefore) {
        this.fromAccountBalanceBefore = fromAccountBalanceBefore;
    }

    public BigDecimal getFromAccountBalanceAfter() {
        return fromAccountBalanceAfter;
    }

    public void setFromAccountBalanceAfter(BigDecimal fromAccountBalanceAfter) {
        this.fromAccountBalanceAfter = fromAccountBalanceAfter;
    }

    public BigDecimal getToAccountBalanceBefore() {
        return toAccountBalanceBefore;
    }

    public void setToAccountBalanceBefore(BigDecimal toAccountBalanceBefore) {
        this.toAccountBalanceBefore = toAccountBalanceBefore;
    }

    public BigDecimal getToAccountBalanceAfter() {
        return toAccountBalanceAfter;
    }

    public void setToAccountBalanceAfter(BigDecimal toAccountBalanceAfter) {
        this.toAccountBalanceAfter = toAccountBalanceAfter;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}