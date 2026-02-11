package com.company.mts.entity;

import com.company.mts.exception.InactiveAccountException;
import com.company.mts.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 12)
    private String accountNumber;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Version
    private Integer version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUpdated;

    // Constructors
    public Account() {
    }

    public Account(Long id, String accountNumber, String holderName, BigDecimal balance,
            AccountStatus status, Integer version, LocalDateTime createdAt, LocalDateTime lastUpdated) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    // Builder pattern
    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    public static class AccountBuilder {
        private Long id;
        private String accountNumber;
        private String holderName;
        private BigDecimal balance;
        private AccountStatus status;
        private Integer version;
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdated;

        public AccountBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AccountBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public AccountBuilder holderName(String holderName) {
            this.holderName = holderName;
            return this;
        }

        public AccountBuilder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public AccountBuilder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public AccountBuilder version(Integer version) {
            this.version = version;
            return this;
        }

        public AccountBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AccountBuilder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Account build() {
            return new Account(id, accountNumber, holderName, balance, status, version, createdAt, lastUpdated);
        }
    }

    @PrePersist
    public void prePersist() {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        lastUpdated = LocalDateTime.now();

        // Validate holder name is not empty
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name cannot be empty");
        }

        // Validate initial balance is not negative
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // =========================
    // Business Methods
    // =========================

    /**
     * Debit (withdraw) money from account
     * Banking rules:
     * - Account must be ACTIVE
     * - Amount must be positive
     * - Sufficient balance required
     */
    public void debit(BigDecimal amount) {
        validateAmount(amount);
        validateAccountIsActive();
        validateSufficientBalance(amount);

        this.balance = this.balance.subtract(amount);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Credit (deposit) money to account
     * Banking rules:
     * - Account must be ACTIVE
     * - Amount must be positive
     */
    public void credit(BigDecimal amount) {
        validateAmount(amount);
        validateAccountIsActive();

        this.balance = this.balance.add(amount);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Lock the account (prevents all transactions)
     */
    public void lock() {
        this.status = AccountStatus.LOCKED;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Close the account (permanently)
     */
    public void close() {
        if (!this.balance.equals(BigDecimal.ZERO)) {
            throw new IllegalStateException(
                    "Cannot close account with non-zero balance. Current balance: " + this.balance);
        }
        this.status = AccountStatus.CLOSED;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Activate/Reactivate the account
     */
    public void activate() {
        if (this.status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot reactivate a closed account");
        }
        this.status = AccountStatus.ACTIVE;
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }

    public boolean isLocked() {
        return AccountStatus.LOCKED.equals(this.status);
    }

    public boolean isClosed() {
        return AccountStatus.CLOSED.equals(this.status);
    }

    // =========================
    // Validation Methods
    // =========================

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }

        // Banking rule: Maximum transaction limit (example: 1 million)
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds maximum limit of 1,000,000");
        }
    }

    private void validateAccountIsActive() {
        if (this.status == AccountStatus.LOCKED) {
            throw new InactiveAccountException("Account is LOCKED. Please contact support to unlock your account.");
        }
        if (this.status == AccountStatus.CLOSED) {
            throw new InactiveAccountException("Account is CLOSED. No transactions allowed on closed accounts.");
        }
        if (!isActive()) {
            throw new InactiveAccountException("Account is not active. Current status: " + this.status);
        }
    }

    private void validateSufficientBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %.2f, Required: %.2f",
                            this.balance, amount));
        }
    }

    // =========================
    // Getters and Setters
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}