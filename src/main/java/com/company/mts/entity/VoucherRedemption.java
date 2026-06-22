package com.company.mts.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_redemptions", indexes = {
    @Index(name = "idx_redemption_account", columnList = "accountId"),
    @Index(name = "idx_redemption_date", columnList = "redeemedAt")
})
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Long voucherId;

    @Column(length = 50)
    private String voucherCode;

    @Column(nullable = false, length = 200)
    private String voucherName;

    @Column(nullable = false)
    private int pointsSpent;

    @Column(unique = true, nullable = false, length = 36)
    private String redemptionCode;

    @Column(nullable = false)
    private LocalDateTime redeemedAt;

    @Column(length = 50)
    private String status = "ACTIVE";

    public VoucherRedemption() {}

    @PrePersist
    public void prePersist() {
        if (redeemedAt == null) redeemedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getVoucherName() { return voucherName; }
    public void setVoucherName(String voucherName) { this.voucherName = voucherName; }
    public int getPointsSpent() { return pointsSpent; }
    public void setPointsSpent(int pointsSpent) { this.pointsSpent = pointsSpent; }
    public String getRedemptionCode() { return redemptionCode; }
    public void setRedemptionCode(String redemptionCode) { this.redemptionCode = redemptionCode; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
