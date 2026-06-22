package com.company.mts.dto;

public class VoucherClaimRequest {
    private Long voucherId;
    private Long accountId;

    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
