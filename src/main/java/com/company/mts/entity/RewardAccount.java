package com.company.mts.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RewardAccount - per-account running total of reward points.
 * Acts as a wallet for accumulated points.
 */
@Entity
@Table(name = "reward_accounts")
public class RewardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One-to-one link to the bank account. */
    @Column(nullable = false, unique = true)
    private Long accountId;

    /** Cumulative points earned by this account. */
    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ----- Constructors -----

    public RewardAccount() {}

    public RewardAccount(Long accountId) {
        this.accountId   = accountId;
        this.totalPoints = 0;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (totalPoints == null) totalPoints = 0;
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ----- Convenience -----

    public void addPoints(int pts) {
        this.totalPoints += pts;
    }

    public void deductPoints(int pts) {
        if (pts > this.totalPoints) {
            throw new IllegalStateException("Insufficient points");
        }
        this.totalPoints -= pts;
    }

    // ----- Getters / Setters -----

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getAccountId()                  { return accountId; }
    public void setAccountId(Long a)            { this.accountId = a; }

    public Integer getTotalPoints()             { return totalPoints; }
    public void setTotalPoints(Integer pts)     { this.totalPoints = pts; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)   { this.updatedAt = t; }
}
