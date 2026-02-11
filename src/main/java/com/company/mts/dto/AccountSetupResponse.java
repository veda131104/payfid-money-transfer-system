package com.company.mts.dto;

public class AccountSetupResponse {
    private Long id;
    private String accountNumber;
    private String upiId;

    public AccountSetupResponse() {}

    public AccountSetupResponse(Long id, String accountNumber, String upiId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.upiId = upiId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
}
