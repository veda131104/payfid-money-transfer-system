package com.company.mts.exception;

public class DuplicateTransactionException extends RuntimeException {
    private final String errorCode;
    private final Long existingTransactionId;

    public DuplicateTransactionException(String message, Long existingTransactionId) {
        super(message);
        this.errorCode = "TRX-409";
        this.existingTransactionId = existingTransactionId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Long getExistingTransactionId() {
        return existingTransactionId;
    }
}