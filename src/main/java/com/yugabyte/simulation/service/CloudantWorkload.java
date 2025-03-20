package com.yugabyte.simulation.service;

import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class CloudantWorkload extends WorkloadSimulationBase implements WorkloadSimulation {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceManager serviceManager;

    @Value("${SPRING_APPLICATION_NAME:}")
    private String applicationName;

    @Override
    public String getName() {
        return "Cloudant" + ((applicationName != null && !applicationName.equals("")) ? " [" + applicationName + "]" : "");
    }

    private static final String DROP_TRANSACTIONS_TABLE = "DROP TABLE IF EXISTS transactions CASCADE;";
    private static final String DROP_USERS_TABLE = "DROP TABLE IF EXISTS users CASCADE;";

    private static final String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE transactions (\n" +
            "    transaction_id UUID PRIMARY KEY,\n" +
            "    \"user\" UUID NOT NULL,\n" +
            "    walletId TEXT,\n" +
            "    type TEXT,\n" +
            "    device UUID,\n" +
            "    amount DECIMAL(10, 2) NOT NULL,\n" +
            "    item TEXT NOT NULL,\n" +
            "    time TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
            "    surcharge DECIMAL(10, 2),\n" +
            "    offer JSONB,\n" +
            "    status INT NOT NULL,\n" +
            "    authKey TEXT,\n" +
            "    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP\n" +
            ");";

    private static final String CREATE_USERS_TABLE = "CREATE TABLE users (\n" +
            "    user_id UUID PRIMARY KEY,\n" +
            "    auth JSONB,\n" +
            "    auth_key TEXT,\n" +
            "    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,\n" +
            "    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP\n" +
            ");";

    private static final String CREATE_INDEX_TRANSACTIONS_USER_STATUS_AMOUNT_WALLETID = "CREATE INDEX idx_transactions_user_status_amount_walletid \n" +
            "ON transactions (\"user\", status, amount, walletId);";

    private static final String CREATE_INDEX_TRANSACTIONS_USER_STATUS_AMOUNT_WALLETID_PARTIAL = "CREATE INDEX idx_transactions_user_status_amount_walletid_partial \n" +
            "ON transactions (\"user\", status, amount, walletId)\n" +
            "WHERE status NOT IN (1, 2) AND amount IS NOT NULL AND \"user\" IS NOT NULL;";

    private static final String CREATE_INDEX_TRANSACTIONS_STATUS_AMOUNT_USER_DEVICE = "CREATE INDEX idx_transactions_status_amount_user_device \n" +
            "ON transactions (status, amount, \"user\", device);";

    private static final String INSERT_USER_RECORD = "INSERT INTO users (user_id, auth, auth_key) VALUES (?, ?::jsonb, ?);";

    private static final String INSERT_TRANSACTION_RECORD = "INSERT INTO transactions (transaction_id, \"user\", walletId, type, device, amount, item, time, surcharge, offer, status, authKey) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?);";

    private static final String SELECT_TRANSACTIONS_QUERY = "SELECT \"user\", \n" +
            "       COALESCE(\n" +
            "           CASE \n" +
            "               WHEN walletId IN ('prepaid_usd', 'prepaid_cad') THEN type \n" +
            "               ELSE walletId \n" +
            "           END, \n" +
            "           type\n" +
            "       ) AS wallet_key, \n" +
            "       SUM(\n" +
            "           CASE \n" +
            "               WHEN status NOT IN (1, 2) AND amount IS NOT NULL AND \"user\" IS NOT NULL THEN \n" +
            "                   CASE \n" +
            "                       WHEN device IS NOT NULL THEN \n" +
            "                           -(amount + COALESCE(surcharge, 0) - COALESCE(offer->>'offerAmount', '0')::numeric)\n" +
            "                       ELSE \n" +
            "                           amount + COALESCE(surcharge, 0) - COALESCE(offer->>'offerAmount', '0')::numeric\n" +
            "                   END \n" +
            "               ELSE 0 \n" +
            "           END\n" +
            "       ) AS balance \n" +
            "FROM transactions \n" +
            "WHERE status NOT IN (1, 2) \n" +
            "  AND amount IS NOT NULL \n" +
            "  AND \"user\" IS NOT NULL \n" +
            "  AND \"user\" = ? \n" +
            "GROUP BY \"user\", wallet_key \n" +
            "ORDER BY \"user\", wallet_key;";

    private static final int ROWS_TO_PRELOAD = 10000;

    private enum WorkloadType {
        CREATE_TABLES,
        SEED_DATA,
        RUN_SIMULATION_FIXED_WORKLOAD
    }

    private final FixedStepsWorkloadType createTablesWorkloadType;
    private final FixedTargetWorkloadType seedingWorkloadType;
    private final FixedTargetWorkloadType simulationFixedWorkloadType;

    public CloudantWorkload() {
        this.createTablesWorkloadType = new FixedStepsWorkloadType(
                new Step("Drop transactions table", (a, b) -> jdbcTemplate.execute(DROP_TRANSACTIONS_TABLE)),
                new Step("Drop users table", (a, b) -> jdbcTemplate.execute(DROP_USERS_TABLE)),
                new Step("Create transactions table", (a, b) -> jdbcTemplate.execute(CREATE_TRANSACTIONS_TABLE)),
                new Step("Create users table", (a, b) -> jdbcTemplate.execute(CREATE_USERS_TABLE)),
                new Step("Create index on transactions (user, status, amount, walletId)", (a, b) -> jdbcTemplate.execute(CREATE_INDEX_TRANSACTIONS_USER_STATUS_AMOUNT_WALLETID)),
                new Step("Create partial index on transactions (user, status, amount, walletId)", (a, b) -> jdbcTemplate.execute(CREATE_INDEX_TRANSACTIONS_USER_STATUS_AMOUNT_WALLETID_PARTIAL)),
                new Step("Create index on transactions (status, amount, user, device)", (a, b) -> jdbcTemplate.execute(CREATE_INDEX_TRANSACTIONS_STATUS_AMOUNT_USER_DEVICE)),
                new Step("Populate Users", (a, b) -> populateUsers())
        );

        this.seedingWorkloadType = new FixedTargetWorkloadType();
        this.simulationFixedWorkloadType = new FixedTargetWorkloadType();
    }

    private WorkloadDesc createTablesWorkload = new WorkloadDesc(
            CloudantWorkload.WorkloadType.CREATE_TABLES.toString(),
            "Create Tables",
            "Create the database tables. If the table already exists it will be dropped"
    );

    private WorkloadDesc seedingWorkload = new WorkloadDesc(
            CloudantWorkload.WorkloadType.SEED_DATA.toString(),
            "Seed Data",
            "Load data into the transactions table",
            new WorkloadParamDesc("Items to generate:", 1, Integer.MAX_VALUE, 10000),
            new WorkloadParamDesc("Threads", 1, Integer.MAX_VALUE, 32)
    );

    private WorkloadDesc simulationFixedWorkload = new WorkloadDesc(
            CloudantWorkload.WorkloadType.RUN_SIMULATION_FIXED_WORKLOAD.toString(),
            "Simulation",
            "Run a simulation of reads on transactions",
            new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000000),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64)
    );

    @Override
    public List<WorkloadDesc> getWorkloads() {
        return Arrays.asList(
                createTablesWorkload,
                seedingWorkload,
                simulationFixedWorkload
        );
    }

    @Override
    public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
        CloudantWorkload.WorkloadType type = CloudantWorkload.WorkloadType.valueOf(workloadId);
        try {
            switch (type) {
                case CREATE_TABLES:
                    this.createTables();
                    return new InvocationResult("Ok");
                case SEED_DATA:
                    this.seedData(values[0].getIntValue(), values[1].getIntValue());
                    return new InvocationResult("Ok");
                case RUN_SIMULATION_FIXED_WORKLOAD:
                    this.runSimulationFixedWorkload(values);
                    return new InvocationResult("Ok");
            }
            throw new IllegalArgumentException("Unknown workload " + workloadId);
        } catch (Exception e) {
            return new InvocationResult(e);
        }
    }

    private void createTables() {
        createTablesWorkloadType.createInstance(serviceManager).execute();
    }

    private void populateUsers() {
        for (int i = 0; i < ROWS_TO_PRELOAD; i++) {
            UUID userId = LoadGeneratorUtils.getUUID();
            String auth = LoadGeneratorUtils.getJson();
            String authKey = LoadGeneratorUtils.getText(10, 20);
            jdbcTemplate.update(INSERT_USER_RECORD, userId, auth, authKey);
        }
    }

    private void seedData(int numberToGenerate, int threads) {
        List<UUID> userIds = new ArrayList<>();
        String query = "SELECT user_id FROM users LIMIT "+ROWS_TO_PRELOAD;
        jdbcTemplate.query(query, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                userIds.add(UUID.fromString(rs.getString("user_id")));
            }
        });
        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(threads, numberToGenerate, (customData, threadData) -> {
                    UUID userId = userIds.get(ThreadLocalRandom.current().nextInt(userIds.size()));
                    runInserts(userId);
                    return threadData;
                });
    }

    private void runSimulationFixedWorkload(ParamValue[] values) {
        int numOfInvocations = values[0].getIntValue();
        int maxThreads = values[1].getIntValue();
        List<UUID> userIds = new ArrayList<>();
        String query = "SELECT user_id FROM users LIMIT "+ROWS_TO_PRELOAD;
        jdbcTemplate.query(query, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                userIds.add(UUID.fromString(rs.getString("user_id")));
            }
        });
        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(maxThreads, numOfInvocations, (customData, threadData) -> {
                    UUID userId = userIds.get(ThreadLocalRandom.current().nextInt(userIds.size()));
                    runSelectQuery(userId);
                    return threadData;
                });
    }

    private void runInserts(UUID userId) {
        UUID transactionId = LoadGeneratorUtils.getUUID();
        String walletId = LoadGeneratorUtils.getText(10, 20);
        String type = LoadGeneratorUtils.getText(5, 10);
        UUID device = LoadGeneratorUtils.getUUID();
        double amount = LoadGeneratorUtils.getDouble(1.00, 1000.00);
        String item = LoadGeneratorUtils.getText(10, 40);
        Date time = LoadGeneratorUtils.getTimestamp();
        double surcharge = LoadGeneratorUtils.getDouble(0.00, 10.00);
        String offer = LoadGeneratorUtils.getJson();
        int status = LoadGeneratorUtils.getInt(0, 2);
        String authKey = LoadGeneratorUtils.getText(10, 20);
        jdbcTemplate.update(INSERT_TRANSACTION_RECORD, transactionId, userId, walletId, type, device, amount, item, time, surcharge, offer, status, authKey);
    }

    private void runSelectQuery(UUID userId) {
        jdbcTemplate.query(SELECT_TRANSACTIONS_QUERY, new Object[] {userId}, new int[] {Types.OTHER}, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                // Process the result set
//                System.out.printf("User: %s, Wallet Key: %s, Balance: %s%n",
//                        rs.getString("user"),       // "user" column
//                        rs.getString("wallet_key"), // "wallet_key" alias
//                        rs.getBigDecimal("balance") // "balance" alias
//                );

            }
        });
    }
}