-- Module 5: Snowflake - Data Analytics
-- Task 6: Analytics Queries

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA ANALYTICS;

-- 1. Daily Transaction Volume
-- Sum of successful transaction amounts by date
SELECT 
    d.full_date,
    COUNT(f.transaction_key) as txn_count,
    SUM(f.amount) as total_volume
FROM FACT_TRANSACTIONS f
JOIN DIM_DATE d ON f.date_key = d.date_key
WHERE f.status = 'SUCCESS'
GROUP BY d.full_date
ORDER BY d.full_date DESC;

-- 2. Account Activity
-- Most active accounts by transaction count
SELECT 
    a.holder_name,
    COUNT(f.transaction_key) as total_txns,
    SUM(f.amount) as total_value
FROM FACT_TRANSACTIONS f
JOIN DIM_ACCOUNT a ON f.account_from_key = a.account_key
GROUP BY a.holder_name
ORDER BY total_txns DESC
LIMIT 10;

-- 3. Success Rate
-- Percentage of successful vs failed transfers
SELECT 
    status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM FACT_TRANSACTIONS
GROUP BY status;

-- 4. Peak Hours
-- Busiest transaction times (extracted from raw timestamp if available, or assumed from fact)
-- Note: Raw transaction_date is in STG_TRANSACTIONS
SELECT 
    EXTRACT(HOUR FROM transaction_date) as hour_of_day,
    COUNT(*) as transaction_count
FROM STG_TRANSACTIONS
GROUP BY 1
ORDER BY 2 DESC;

-- 5. Average Transfer Amount
-- Mean transfer value for successful transactions
SELECT 
    AVG(amount) as avg_transfer_value,
    MIN(amount) as min_transfer_value,
    MAX(amount) as max_transfer_value
FROM FACT_TRANSACTIONS
WHERE status = 'SUCCESS';
