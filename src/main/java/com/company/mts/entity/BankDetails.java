package com.company.mts.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_details")
public class BankDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String ifscCode;

    @Column(nullable = false)
    private String contact; // phone or email for OTP

    private String creditCardNumber;

    private String cvv;

    @Column(unique = true)
    private String upiId;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    public BankDetails() {
    }

    public BankDetails(Long id, String accountNumber, String bankName, String ifscCode, String contact, String creditCardNumber, String cvv, String upiId, LocalDateTime createdAt, LocalDateTime lastUpdated) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
        this.contact = contact;
        this.creditCardNumber = creditCardNumber;
        this.cvv = cvv;
        this.upiId = upiId;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String accountNumber;
        private String bankName;
        private String ifscCode;
        private String contact;
        private String creditCardNumber;
        private String cvv;
        private String upiId;
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdated;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public Builder bankName(String bankName) { this.bankName = bankName; return this; }
        public Builder ifscCode(String ifscCode) { this.ifscCode = ifscCode; return this; }
        public Builder contact(String contact) { this.contact = contact; return this; }
        public Builder creditCardNumber(String creditCardNumber) { this.creditCardNumber = creditCardNumber; return this; }
        public Builder cvv(String cvv) { this.cvv = cvv; return this; }
        public Builder upiId(String upiId) { this.upiId = upiId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder lastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; return this; }

        public BankDetails build() { return new BankDetails(id, accountNumber, bankName, ifscCode, contact, creditCardNumber, cvv, upiId, createdAt, lastUpdated); }
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { lastUpdated = LocalDateTime.now(); }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getCreditCardNumber() { return creditCardNumber; }
    public void setCreditCardNumber(String creditCardNumber) { this.creditCardNumber = creditCardNumber; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
