-- Module 5: Snowflake - Data Analytics
-- Task 3 & 4: Dimension and Fact Tables

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA ANALYTICS;

-- Dimension: DIM_ACCOUNT
CREATE OR REPLACE TABLE DIM_ACCOUNT (
    account_key NUMBER AUTOINCREMENT,
    account_id NUMBER,
    holder_name STRING,
    status STRING,
    effective_date TIMESTAMP_NTZ DEFAULT CURRENT_TIMESTAMP(),
    PRIMARY KEY (account_key)
);

-- Dimension: DIM_DATE
CREATE OR REPLACE TABLE DIM_DATE (
    date_key NUMBER,
    full_date DATE,
    day NUMBER,
    month NUMBER,
    year NUMBER,
    quarter NUMBER,
    PRIMARY KEY (date_key)
);

-- Fact: FACT_TRANSACTIONS
CREATE OR REPLACE TABLE FACT_TRANSACTIONS (
    transaction_key NUMBER AUTOINCREMENT,
    transaction_id NUMBER,
    account_from_key NUMBER,
    account_to_key NUMBER,
    date_key NUMBER,
    amount NUMBER(38, 2),
    status STRING,
    PRIMARY KEY (transaction_key),
    FOREIGN KEY (account_from_key) REFERENCES DIM_ACCOUNT(account_key),
    FOREIGN KEY (account_to_key) REFERENCES DIM_ACCOUNT(account_key),
    FOREIGN KEY (date_key) REFERENCES DIM_DATE(date_key)
);

-- Staging table for raw data sync
CREATE OR REPLACE TABLE STG_TRANSACTIONS (
    id NUMBER,
    from_account_id NUMBER,
    to_account_id NUMBER,
    amount NUMBER(38, 2),
    type STRING,
    status STRING,
    transaction_date TIMESTAMP_NTZ
);
