package com.company.mts.dto;

import com.company.mts.entity.TransactionStatus;
import com.company.mts.entity.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
public class TransactionDTO {

    // Getters and Setters
    private Long id;
    private Long fromAccountId;
    private String fromAccountNumber;
    private String fromAccountHolderName;
    private Long toAccountId;
    private String toAccountNumber;
    private String toAccountHolderName;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime transactionDate;
    private String description;
    private BigDecimal fromAccountBalanceBefore;
    private BigDecimal fromAccountBalanceAfter;
    private BigDecimal toAccountBalanceBefore;
    private BigDecimal toAccountBalanceAfter;
    private String failureReason;
    private String idempotencyKey;


    // Constructors
    public TransactionDTO() {
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long fromAccountId;
        private String fromAccountNumber;
        private String fromAccountHolderName;
        private Long toAccountId;
        private String toAccountNumber;
        private String toAccountHolderName;
        private BigDecimal amount;
        private TransactionType type;
        private TransactionStatus status;
        private LocalDateTime transactionDate;
        private String description;
        private BigDecimal fromAccountBalanceBefore;
        private BigDecimal fromAccountBalanceAfter;
        private BigDecimal toAccountBalanceBefore;
        private BigDecimal toAccountBalanceAfter;
        private String failureReason;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder fromAccountId(Long fromAccountId) {
            this.fromAccountId = fromAccountId;
            return this;
        }

        public Builder fromAccountNumber(String fromAccountNumber) {
            this.fromAccountNumber = fromAccountNumber;
            return this;
        }

        public Builder fromAccountHolderName(String fromAccountHolderName) {
            this.fromAccountHolderName = fromAccountHolderName;
            return this;
        }

        public Builder toAccountId(Long toAccountId) {
            this.toAccountId = toAccountId;
            return this;
        }

        public Builder toAccountNumber(String toAccountNumber) {
            this.toAccountNumber = toAccountNumber;
            return this;
        }

        public Builder toAccountHolderName(String toAccountHolderName) {
            this.toAccountHolderName = toAccountHolderName;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder transactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder fromAccountBalanceBefore(BigDecimal fromAccountBalanceBefore) {
            this.fromAccountBalanceBefore = fromAccountBalanceBefore;
            return this;
        }

        public Builder fromAccountBalanceAfter(BigDecimal fromAccountBalanceAfter) {
            this.fromAccountBalanceAfter = fromAccountBalanceAfter;
            return this;
        }

        public Builder toAccountBalanceBefore(BigDecimal toAccountBalanceBefore) {
            this.toAccountBalanceBefore = toAccountBalanceBefore;
            return this;
        }

        public Builder toAccountBalanceAfter(BigDecimal toAccountBalanceAfter) {
            this.toAccountBalanceAfter = toAccountBalanceAfter;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public TransactionDTO build() {
            TransactionDTO dto = new TransactionDTO();
            dto.id = this.id;
            dto.fromAccountId = this.fromAccountId;
            dto.fromAccountNumber = this.fromAccountNumber;
            dto.fromAccountHolderName = this.fromAccountHolderName;
            dto.toAccountId = this.toAccountId;
            dto.toAccountNumber = this.toAccountNumber;
            dto.toAccountHolderName = this.toAccountHolderName;
            dto.amount = this.amount;
            dto.type = this.type;
            dto.status = this.status;
            dto.transactionDate = this.transactionDate;
            dto.description = this.description;
            dto.fromAccountBalanceBefore = this.fromAccountBalanceBefore;
            dto.fromAccountBalanceAfter = this.fromAccountBalanceAfter;
            dto.toAccountBalanceBefore = this.toAccountBalanceBefore;
            dto.toAccountBalanceAfter = this.toAccountBalanceAfter;
            dto.failureReason = this.failureReason;
            return dto;
        }
    }

}