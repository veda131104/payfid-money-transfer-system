-- ==========================================================
-- PayFid Money Transfer System - Database Seed Script
-- ==========================================================
-- Run this script on your MySQL database ('money_transfer_system')
-- to populate it with realistic mock data for testing.
-- All passwords are set to 'Password123' (BCrypt hashed).

USE money_transfer_system;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE reward_ledger;
TRUNCATE TABLE reward_accounts;
TRUNCATE TABLE transaction_logs;
TRUNCATE TABLE bank_details;
TRUNCATE TABLE accounts;
TRUNCATE TABLE auth_users;

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
-- 1. Populate Auth Users (password: 'Password123')
-- ==========================================
INSERT INTO auth_users (id, name, email, password, first_login, role, created_on)
VALUES
(1, 'Sravya', 'sravyad13@gmail.com', '$2a$10$6pG9u0GOlb0gpEudB/ZJ7.shR8p0Sk/MFu82C3Gp4ehj.TdZKCXyG', false, 'USER', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 'Jane Smith', 'jane.smith@example.com', '$2a$10$6pG9u0GOlb0gpEudB/ZJ7.shR8p0Sk/MFu82C3Gp4ehj.TdZKCXyG', false, 'USER', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 'John Doe', 'john.doe@example.com', '$2a$10$6pG9u0GOlb0gpEudB/ZJ7.shR8p0Sk/MFu82C3Gp4ehj.TdZKCXyG', false, 'USER', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, 'Alice', 'alice.vance@example.com', '$2a$10$6pG9u0GOlb0gpEudB/ZJ7.shR8p0Sk/MFu82C3Gp4ehj.TdZKCXyG', false, 'USER', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(5, 'Bob', 'bob.miller@example.com', '$2a$10$6pG9u0GOlb0gpEudB/ZJ7.shR8p0Sk/MFu82C3Gp4ehj.TdZKCXyG', false, 'USER', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, 'Charlie', 'charlie.brown@example.com', '$2a$10$6pG9u0GOlb0gpEudB/ZJ7.shR8p0Sk/MFu82C3Gp4ehj.TdZKCXyG', false, 'USER', DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ==========================================
-- 2. Populate Bank Accounts
-- ==========================================
INSERT INTO accounts (id, account_number, holder_name, balance, status, version, created_on, last_updated)
VALUES
(1, '123456789012', 'Sravya', 50000.00, 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(2, '987654321098', 'Jane Smith', 7500.00, 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(3, '111122223333', 'John Doe', 10000.00, 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(4, '444455556666', 'Alice', 120000.00, 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(5, '777788889999', 'Bob', 350000.00, 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(6, '555544443333', 'Charlie', 20000.00, 'ACTIVE', 0, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW());

-- ==========================================
-- 3. Populate Bank Details
-- ==========================================
INSERT INTO bank_details (id, account_number, bank_name, ifsc_code, branch_name, address, email, contact, user_name, credit_card_number, cvv, expiry_date, upi_id, pin, created_on, last_updated)
VALUES
(1, '123456789012', 'HDFC Bank', 'HDFC0000123', 'MG Road Branch', '123 MG Road, Bangalore', 'sravyad13@gmail.com', '9876543210', 'Sravya', '4321567890123456', '123', '12/28', 'sravya@payfid', '1234', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(2, '987654321098', 'ICICI Bank', 'ICIC0000987', 'Whitefield Branch', '45 Whitefield Road, Bangalore', 'jane.smith@example.com', '9888877777', 'Jane Smith', '9876543210987654', '456', '08/29', 'janesmith@payfid', '5678', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(3, '111122223333', 'State Bank of India', 'SBIN0001111', 'Koramangala Branch', 'SB Road, Bangalore', 'john.doe@example.com', '9777766666', 'John Doe', '1111222233334444', '789', '04/30', 'johndoe@payfid', '1111', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(4, '444455556666', 'Axis Bank', 'UTIB0000444', 'Indiranagar Branch', '80 Feet Road, Bangalore', 'alice.vance@example.com', '9666655555', 'Alice', '4444555566667777', '321', '10/27', 'alice@payfid', '4321', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(5, '777788889999', 'Kotak Bank', 'KKBK0000777', 'Jayanagar Branch', '3rd Block Jayanagar, Bangalore', 'bob.miller@example.com', '9555544444', 'Bob', '7777888899990000', '999', '05/31', 'bob@payfid', '9999', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(6, '555544443333', 'Punjab National Bank', 'PUNB0000555', 'Malleswaram Branch', 'Sampige Road, Bangalore', 'charlie.brown@example.com', '9444433333', 'Charlie', '5555444433332222', '111', '11/29', 'charlie@payfid', '2222', DATE_SUB(NOW(), INTERVAL 9 DAY), NOW());

-- ==========================================
-- 4. Populate Transactions
-- ==========================================
INSERT INTO transaction_logs (id, from_account_id, to_account_id, amount, type, status, transaction_date, description, from_account_balance_before, from_account_balance_after, to_account_balance_before, to_account_balance_after, idempotency_key)
VALUES
-- Sravya (1) transfers to Jane Smith (2) - ₹1500 (15 points)
(1, 1, 2, 1500.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 8 DAY), 'Rent Payment', 52700.00, 51200.00, 6000.00, 7500.00, 'idem-1111'),
-- Sravya (1) transfers to John Doe (3) - ₹250 (2 points)
(2, 1, 3, 250.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 7 DAY), 'Dinner share split', 51200.00, 50950.00, 9750.00, 10000.00, 'idem-2222'),
-- John Doe (3) transfers to Sravya (1) - ₹500 (5 points)
(3, 3, 1, 500.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 6 DAY), 'Gift reimbursement', 10500.00, 10000.00, 50450.00, 50950.00, 'idem-3333'),
-- Sravya (1) makes external transfer - ₹1200 (12 points)
(4, 1, 1, 1200.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY), 'External: Online shopping payment', 50950.00, 49750.00, 50950.00, 49750.00, 'idem-4444'),
-- Charlie (6) transfers to Alice (4) - ₹3500 (35 points)
(5, 6, 4, 3500.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 DAY), 'Consulting payment', 23500.00, 20000.00, 116500.00, 120000.00, 'idem-5555'),
-- Alice (4) transfers to Bob (5) - ₹7500 (75 points)
(6, 4, 5, 7500.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY), 'Investment transfer', 127500.00, 120000.00, 342500.00, 350000.00, 'idem-6666'),
-- Bob (5) transfers to Charlie (6) - ₹15000 (150 points)
(7, 5, 6, 15000.00, 'TRANSFER', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Project payout', 365000.00, 350000.00, 5000.00, 20000.00, 'idem-7777');

-- ==========================================
-- 5. Populate Reward Accounts (Wallets)
-- ==========================================
-- Sravya (1): 15 + 2 + 12 = 29 points (Silver Tier)
-- Jane Smith (2): 0 points (Bronze Tier)
-- John Doe (3): 5 points (Bronze Tier)
-- Alice (4): 75 points (Gold Tier)
-- Bob (5): 150 points (Platinum Tier)
-- Charlie (6): 35 points (Silver Tier)
INSERT INTO reward_accounts (id, account_id, total_points, created_at, updated_at)
VALUES
(1, 1, 29, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(2, 2, 0, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(3, 3, 5, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(4, 4, 75, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(5, 5, 150, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(6, 6, 35, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ==========================================
-- 6. Populate Reward Ledger Audit Trail
-- ==========================================
INSERT INTO reward_ledger (id, account_id, transaction_id, transaction_amount, points_awarded, description, granted_at)
VALUES
(1, 1, 1, 1500.00, 15, 'Points earned for transfer to Jane Smith', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2, 1, 2, 250.00, 2, 'Points earned for Dinner share split', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3, 3, 3, 500.00, 5, 'Points earned for Gift reimbursement', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 1, 4, 1200.00, 12, 'Points earned for External: Online shopping payment', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 6, 5, 3500.00, 35, 'Points earned for transfer to Alice', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 4, 6, 7500.00, 75, 'Points earned for transfer to Bob', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 5, 7, 15000.00, 150, 'Points earned for transfer to Charlie', DATE_SUB(NOW(), INTERVAL 2 DAY));
