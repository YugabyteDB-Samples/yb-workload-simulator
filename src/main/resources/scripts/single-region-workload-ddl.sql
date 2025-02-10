-- Create the main transactions table as a partitioned table
CREATE TABLE transactions (
                              transaction_id UUID NOT NULL,
                              region TEXT NOT NULL,
                              account_id INT NOT NULL,
                              symbol TEXT NOT NULL,
                              amount NUMERIC,
                              transaction_time TIMESTAMP DEFAULT now()
) PARTITION BY LIST (region);

-- Create the first partition (transactions_east1) using us_multi_region_tablespace1
CREATE TABLE transactions_east1 PARTITION OF transactions (
    transaction_id,
    region,
    account_id,
    symbol,
    amount,
    transaction_time,
    PRIMARY KEY (transaction_id HASH, region)
    ) FOR VALUES IN ('us-east-1');

-- Create the second partition (transactions_east2) using us_multi_region_tablespace2
CREATE TABLE transactions_east2 PARTITION OF transactions (
    transaction_id,
    region,
    account_id,
    symbol,
    amount,
    transaction_time,
    PRIMARY KEY (transaction_id HASH, region)
    ) FOR VALUES IN ('us-east-2');

