CREATE TABLESPACE us_multi_region_tablespace1
WITH (
    replica_placement = '{"num_replicas": 5, "placement_blocks": [
     {"cloud": "aws", "region": "us-east-1", "zone": "us-east-1a", "min_num_replicas": 1, "leader_preference": 1},
     {"cloud": "aws", "region": "us-east-1", "zone": "us-east-1b", "min_num_replicas": 1, "leader_preference": 1},
     {"cloud": "aws", "region": "us-east-2", "zone": "us-east-2a", "min_num_replicas": 1, "leader_preference": 2},
     {"cloud": "aws", "region": "us-east-2", "zone": "us-east-2b", "min_num_replicas": 1, "leader_preference": 2},
     {"cloud": "aws", "region": "us-west-2", "zone": "us-west-2a", "min_num_replicas": 1}
   ]}'
);


-- CREATE TABLESPACE east_tablespace WITH (
--     replica_placement='{"num_replicas": 3, "placement_blocks":
-- [{"cloud":"azu","region":"eastus","zone":"eastus-2","min_num_replicas":1,"leader_preference":1},
-- {"cloud":"azu","region":"southcentralus","zone":"southcentralus-2","min_num_replicas":1,"leader_preference":2},
-- {"cloud":"azu","region":"westus3","zone":"westus3-1","min_num_replicas":1}]}'
-- );
--
-- CREATE TABLESPACE west_tablespace WITH (
--     replica_placement='{"num_replicas": 3, "placement_blocks":
--  [{"cloud":"azu","region":"eastus","zone":"eastus-2","min_num_replicas":1},
--  {"cloud":"azu","region":"southcentralus","zone":"southcentralus-2","min_num_replicas":1,"leader_preference":2},
--  {"cloud":"azu","region":"westus3","zone":"westus3-1","min_num_replicas":1, "leader_preference":1}]}'
-- );


CREATE TABLESPACE us_multi_region_tablespace2
WITH (
    replica_placement = '{"num_replicas": 5, "placement_blocks": [
     {"cloud": "aws", "region": "us-east-1", "zone": "us-east-1a", "min_num_replicas": 1, "leader_preference": 2},
     {"cloud": "aws", "region": "us-east-1", "zone": "us-east-1b", "min_num_replicas": 1, "leader_preference": 2},
     {"cloud": "aws", "region": "us-east-2", "zone": "us-east-2a", "min_num_replicas": 1, "leader_preference": 1},
     {"cloud": "aws", "region": "us-east-2", "zone": "us-east-2b", "min_num_replicas": 1, "leader_preference": 1},
     {"cloud": "aws", "region": "us-west-2", "zone": "us-west-2a", "min_num_replicas": 1}
   ]}'
);

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
    ) FOR VALUES IN ('us-east-1') TABLESPACE us_multi_region_tablespace1;

-- Create the second partition (transactions_east2) using us_multi_region_tablespace2
CREATE TABLE transactions_east2 PARTITION OF transactions (
    transaction_id,
    region,
    account_id,
    symbol,
    amount,
    transaction_time,
    PRIMARY KEY (transaction_id HASH, region)
    ) FOR VALUES IN ('us-east-2') TABLESPACE us_multi_region_tablespace2;

