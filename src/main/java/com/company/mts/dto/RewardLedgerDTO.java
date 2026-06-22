package com.company.mts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO returned for each entry in the reward ledger.
 */
public class RewardLedgerDTO {

    private Long id;
    private Long accountId;
    private Long transactionId;
    private BigDecimal transactionAmount;
    private Integer pointsAwarded;
    private String description;
    private LocalDateTime grantedAt;

    public RewardLedgerDTO() {}

    // ----- Getters / Setters -----

    public Long getId()                             { return id; }
    public void setId(Long id)                      { this.id = id; }

    public Long getAccountId()                      { return accountId; }
    public void setAccountId(Long a)                { this.accountId = a; }

    public Long getTransactionId()                  { return transactionId; }
    public void setTransactionId(Long t)            { this.transactionId = t; }

    public BigDecimal getTransactionAmount()        { return transactionAmount; }
    public void setTransactionAmount(BigDecimal t)  { this.transactionAmount = t; }

    public Integer getPointsAwarded()               { return pointsAwarded; }
    public void setPointsAwarded(Integer pts)       { this.pointsAwarded = pts; }

    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }

    public LocalDateTime getGrantedAt()             { return grantedAt; }
    public void setGrantedAt(LocalDateTime t)       { this.grantedAt = t; }
}
