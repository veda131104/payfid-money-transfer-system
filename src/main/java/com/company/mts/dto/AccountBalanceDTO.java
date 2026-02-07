package com.company.mts.dto;

import java.math.BigDecimal;

public class AccountBalanceDTO {

    private Long accountId;
    private String accountNumber;
    private String holderName;
    private BigDecimal balance;
    private String status;

    public AccountBalanceDTO() {
    }

    public AccountBalanceDTO(Long accountId, String accountNumber, String holderName,
                             BigDecimal balance, String status) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
        this.status = status;
    }

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}