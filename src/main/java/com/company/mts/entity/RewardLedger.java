package com.company.mts.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RewardLedger - tracks every reward event tied to a transaction.
 * One row per eligible transaction; provides full audit trail.
 */
@Entity
@Table(name = "reward_ledger", indexes = {
        @Index(name = "idx_reward_account", columnList = "accountId"),
        @Index(name = "idx_reward_transaction", columnList = "transactionId"),
        @Index(name = "idx_reward_granted_at", columnList = "grantedAt")
})
public class RewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The account that earned the reward (the sender). */
    @Column(nullable = false)
    private Long accountId;

    /** The transaction that triggered this reward. */
    @Column(nullable = false, unique = true)
    private Long transactionId;

    /** Amount of the original transaction. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal transactionAmount;

    /** Points awarded for this transaction (floor(amount / 100)). */
    @Column(nullable = false)
    private Integer pointsAwarded;

    /** Human-readable reason / description of the reward. */
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    // ----- Constructors -----

    public RewardLedger() {}

    private RewardLedger(Builder b) {
        this.accountId         = b.accountId;
        this.transactionId     = b.transactionId;
        this.transactionAmount = b.transactionAmount;
        this.pointsAwarded     = b.pointsAwarded;
        this.description       = b.description;
        this.grantedAt         = b.grantedAt;
    }

    @PrePersist
    public void prePersist() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long accountId;
        private Long transactionId;
        private BigDecimal transactionAmount;
        private Integer pointsAwarded;
        private String description;
        private LocalDateTime grantedAt;

        public Builder accountId(Long accountId)                     { this.accountId = accountId; return this; }
        public Builder transactionId(Long transactionId)             { this.transactionId = transactionId; return this; }
        public Builder transactionAmount(BigDecimal transactionAmount){ this.transactionAmount = transactionAmount; return this; }
        public Builder pointsAwarded(Integer pointsAwarded)          { this.pointsAwarded = pointsAwarded; return this; }
        public Builder description(String description)               { this.description = description; return this; }
        public Builder grantedAt(LocalDateTime grantedAt)            { this.grantedAt = grantedAt; return this; }

        public RewardLedger build() { return new RewardLedger(this); }
    }

    // ----- Getters / Setters -----

    public Long getId()                            { return id; }
    public void setId(Long id)                     { this.id = id; }

    public Long getAccountId()                     { return accountId; }
    public void setAccountId(Long accountId)       { this.accountId = accountId; }

    public Long getTransactionId()                 { return transactionId; }
    public void setTransactionId(Long transactionId){ this.transactionId = transactionId; }

    public BigDecimal getTransactionAmount()       { return transactionAmount; }
    public void setTransactionAmount(BigDecimal t) { this.transactionAmount = t; }

    public Integer getPointsAwarded()              { return pointsAwarded; }
    public void setPointsAwarded(Integer pts)      { this.pointsAwarded = pts; }

    public String getDescription()                 { return description; }
    public void setDescription(String d)           { this.description = d; }

    public LocalDateTime getGrantedAt()            { return grantedAt; }
    public void setGrantedAt(LocalDateTime t)      { this.grantedAt = t; }
}
