-- ===============================
-- Money Transfer System - Database Init
-- ===============================

USE money_transfer_system;

-- Drop tables if they exist (clean slate)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS transaction_logs;
DROP TABLE IF EXISTS bank_details;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS auth_users;
SET FOREIGN_KEY_CHECKS = 1;

-- Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(12) NOT NULL UNIQUE,
    holder_name VARCHAR(255) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT DEFAULT 0,
    created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP NULL,
    CONSTRAINT chk_balance CHECK (balance >= 0),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'LOCKED', 'CLOSED'))
);

-- Bank Details Table
CREATE TABLE IF NOT EXISTS bank_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(12) NOT NULL UNIQUE,
    bank_name VARCHAR(255) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    branch_name VARCHAR(255),
    address VARCHAR(500),
    email VARCHAR(255) NOT NULL,
    contact VARCHAR(20) NOT NULL,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    credit_card_number VARCHAR(20),
    cvv VARCHAR(3),
    expiry_date VARCHAR(10),
    upi_id VARCHAR(255) UNIQUE,
    pin VARCHAR(10),
    created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP NULL
);

-- Transaction Logs Table
CREATE TABLE IF NOT EXISTS transaction_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500),
    from_account_balance_before DECIMAL(19, 2),
    from_account_balance_after DECIMAL(19, 2),
    to_account_balance_before DECIMAL(19, 2),
    to_account_balance_after DECIMAL(19, 2),
    idempotency_key VARCHAR(100) UNIQUE,
    failure_reason VARCHAR(1000),
    CONSTRAINT chk_amount CHECK (amount > 0),
    CONSTRAINT chk_type CHECK (type IN ('CREDIT', 'DEBIT', 'TRANSFER')),
    CONSTRAINT chk_tx_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

-- Auth Users Table
CREATE TABLE IF NOT EXISTS auth_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_login BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    recovery_token VARCHAR(255) UNIQUE,
    recovery_token_expiry TIMESTAMP NULL,
    remember_token VARCHAR(255) UNIQUE,
    remember_token_expiry TIMESTAMP NULL,
    created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_account_number ON accounts(account_number);
CREATE INDEX idx_holder_name ON accounts(holder_name);
CREATE INDEX idx_from_account ON transaction_logs(from_account_id);
CREATE INDEX idx_to_account ON transaction_logs(to_account_id);
CREATE INDEX idx_transaction_date ON transaction_logs(transaction_date);
CREATE INDEX idx_idempotency_key ON transaction_logs(idempotency_key);
CREATE INDEX idx_status ON transaction_logs(status);
CREATE INDEX idx_auth_users_email ON auth_users(email);
CREATE INDEX idx_auth_users_name ON auth_users(name);

-- Table comments
ALTER TABLE accounts COMMENT = 'Stores bank account information';
ALTER TABLE transaction_logs COMMENT = 'Audit trail for all transactions';
ALTER TABLE bank_details COMMENT = 'Stores bank and payment details for accounts';
ALTER TABLE auth_users COMMENT = 'Authentication users for the system';
