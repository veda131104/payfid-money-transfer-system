package com.company.mts.dto;

import java.time.LocalDateTime;

/**
 * Summary DTO for a reward account — total points + account metadata.
 */
public class RewardSummaryDTO {

    private Long rewardAccountId;
    private Long accountId;
    private Integer totalPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RewardSummaryDTO() {}

    // ----- Getters / Setters -----

    public Long getRewardAccountId()                  { return rewardAccountId; }
    public void setRewardAccountId(Long id)           { this.rewardAccountId = id; }

    public Long getAccountId()                        { return accountId; }
    public void setAccountId(Long a)                  { this.accountId = a; }

    public Integer getTotalPoints()                   { return totalPoints; }
    public void setTotalPoints(Integer pts)           { this.totalPoints = pts; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime t)         { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)         { this.updatedAt = t; }
}
