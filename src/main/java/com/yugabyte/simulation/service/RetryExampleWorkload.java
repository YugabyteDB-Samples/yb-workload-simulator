package com.yugabyte.simulation.service;

import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.util.QuikShipWorkloadUtil;
import com.yugabyte.simulation.workload.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class RetryExampleWorkload extends WorkloadSimulationBase implements WorkloadSimulation{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceManager serviceManager;

    @Value("${SPRING_APPLICATION_NAME:}")
    private String applicationName;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private RetryOperationsInterceptor ysqlRetryInterceptor;


    @Override
    public String getName() {
        return "RetryExample"+ ((applicationName != null && !applicationName.equals(""))? " ["+applicationName+"]" : "");
    }

    private static final String CREATE_PRODUCT_TYPE = "CREATE TYPE  product_type_enum AS ENUM ('book', 'technology');";

    private static final String CREATE_PRODUCTS = "CREATE TABLE if not exists products(\n" +
            "    id SERIAL PRIMARY KEY,\n" +
            "    title VARCHAR(255),\n" +
            "    author VARCHAR(255),\n" +
            "    imageLink VARCHAR(255),\n" +
            "    price decimal(12, 2),\n" +
            "    product_type product_type_enum\n" +
            ");";

    private static final String CREATE_ORDERS = "CREATE TABLE if not exists orders(\n" +
            "    id SERIAL PRIMARY KEY,\n" +
            "    total decimal(12, 2),\n" +
            "    products VARCHAR(255)\n" +
            ");";



    private static final String DROP_PRODUCT_TYPE = "drop type if exists product_type_enum;";
    private static final String DROP_PRODUCTS = "drop table if exists products cascade;";
    private static final String TRUNCATE_PRODUCTS = "truncate table products;";
    private static final String DROP_ORDERS = "drop table if exists orders cascade;";
    private static final String TRUNCATE_ORDERS = "truncate table orders;";

    private static final String INSERT_RECORD_ORDERS = "insert into orders(total,products) values(?,?);";

    private static final String UPDATE_RECORD_ORDERS = "update orders set total = ? where id = ? ;";

    private final String POINT_SELECT_QUERY_ORDERS = "select id, total, products from orders where id = ?;";

    private static final int ROWS_TO_PRELOAD = 10000;

    private enum WorkloadType {
        CREATE_TABLES,
        SEED_DATA,
        RUN_SIMULATION_FIXED_WORKLOAD,
        RUN_SIMULATION,
        UPDATE_DATA
    }

    private final FixedStepsWorkloadType createTablesWorkloadType;
    private final FixedTargetWorkloadType seedingWorkloadType;
    private final ThroughputWorkloadType runInstanceType;
    private final FixedTargetWorkloadType simulationFixedWorkloadType;
    private final FixedTargetWorkloadType updateFixedWorkloadType;

    public RetryExampleWorkload() {
        this.createTablesWorkloadType = new FixedStepsWorkloadType(
                new Step("Drop orders", (a, b) -> jdbcTemplate.execute(DROP_ORDERS)),
                new Step("Drop products", (a,b) -> jdbcTemplate.execute(DROP_PRODUCTS)),
                new Step("Drop product_type_enum", (a, b) -> jdbcTemplate.execute(DROP_PRODUCT_TYPE)),
                new Step("Create product_type_enum", (a,b) -> jdbcTemplate.execute(CREATE_PRODUCT_TYPE)),
                new Step("Create products", (a,b) -> jdbcTemplate.execute(CREATE_PRODUCTS)),
                new Step("Create orders", (a,b) -> jdbcTemplate.execute(CREATE_ORDERS)),
                new Step("Populate Products", (a,b) -> jdbcTemplate.execute(QuikShipWorkloadUtil.INSERT_PRODUCTS_DATA))
        );

        this.seedingWorkloadType = new FixedTargetWorkloadType();
        this.runInstanceType = new ThroughputWorkloadType();
        this.simulationFixedWorkloadType = new FixedTargetWorkloadType();
        this.updateFixedWorkloadType = new FixedTargetWorkloadType();
    }

    private WorkloadDesc createTablesWorkload = new WorkloadDesc(
            RetryExampleWorkload.WorkloadType.CREATE_TABLES.toString(),
            "Create Tables",
            "Create the database tables. If the table already exists it will be dropped"
    );

    private WorkloadDesc seedingWorkload = new WorkloadDesc(
            RetryExampleWorkload.WorkloadType.SEED_DATA.toString(),
            "Seed Data",
            "Load data into the orders table",
            new WorkloadParamDesc("Items to generate:", 1, Integer.MAX_VALUE, 10000),
            new WorkloadParamDesc("Threads", 1, Integer.MAX_VALUE, 32)
    );

    private WorkloadDesc runningWorkload = new WorkloadDesc(
            RetryExampleWorkload.WorkloadType.RUN_SIMULATION.toString(),
            "Simulation - TPS",
            "Run a simulation of a reads on orders placed",
            new WorkloadParamDesc("Throughput (tps)", 1, 1000000, 500),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64),
            new WorkloadParamDesc("Include placing of new orders (inserts)", false)
    );

    private WorkloadDesc simulationFixedWorkload = new WorkloadDesc(
            RetryExampleWorkload.WorkloadType.RUN_SIMULATION_FIXED_WORKLOAD.toString(),
            "Simulation",
            "Run a simulation of a reads on orders placed",
            new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000000),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64),
            new WorkloadParamDesc("Include placing of new orders (inserts)", false)
    );

    private WorkloadDesc updateWorkload = new WorkloadDesc(
            WorkloadType.UPDATE_DATA.toString(),
            "Update Data",
            "Update the orders table",
            new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000000),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64)
    );


    @Override
    public List<WorkloadDesc> getWorkloads() {
        return Arrays.asList(
                createTablesWorkload
                , seedingWorkload
                , simulationFixedWorkload
                , runningWorkload
                , updateWorkload
        );
    }


    @Override
    public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
        RetryExampleWorkload.WorkloadType type = RetryExampleWorkload.WorkloadType.valueOf(workloadId);
        try {
            switch (type) {
                case CREATE_TABLES:
                    this.createTables();
                    return new InvocationResult("Ok");
                case SEED_DATA:
                    this.seedData(values[0].getIntValue(), values[1].getIntValue());
                    return new InvocationResult("Ok");
                case RUN_SIMULATION:
                    this.runSimulation(values);
                    return new InvocationResult("Ok");
                case RUN_SIMULATION_FIXED_WORKLOAD:
                    this.runSimulationFixedWorkload(values);
                    return new InvocationResult("Ok");
                case UPDATE_DATA:
                    this.runUpdateWorkload(values);
                    return new InvocationResult("Ok");

            }
            throw new IllegalArgumentException("Unknown workload "+ workloadId);
        }
        catch (Exception e) {
            return new InvocationResult(e);
        }
    }

    private void createTables() {
        createTablesWorkloadType.createInstance(serviceManager).execute();
    }

    private void seedData(int numberToGenerate, int threads) {
        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(threads, numberToGenerate, (customData, threadData) -> {
                    runInserts();
                    return threadData;
                });
    }




    private void runSimulationFixedWorkload(ParamValue[] values) {
        int numOfInvocations = values[0].getIntValue();
        int maxThreads = values[1].getIntValue();
        boolean runInserts = values[2].getBoolValue();
        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(maxThreads, numOfInvocations, (customData, threadData) -> {
                    int id = LoadGeneratorUtils.getInt(1,ROWS_TO_PRELOAD);;
                    runPointReadOrders(id);
                    if(runInserts){
                        runInserts();
                    }
                    return threadData;
                });
    }


    public void runUpdateWorkload(ParamValue[] values) {
        int numOfInvocations = values[0].getIntValue();
        int maxThreads = values[1].getIntValue();
        updateFixedWorkloadType
                .createInstance(serviceManager)
                .execute(maxThreads, numOfInvocations, (customData, threadData) -> {
                    runUpdates();
                    return threadData;
                });
    }

    private void runSimulation(ParamValue[] values) {
        int tps = values[0].getIntValue();
        int maxThreads = values[1].getIntValue();
        boolean runInserts = values[2].getBoolValue();

        Random random = ThreadLocalRandom.current();
        jdbcTemplate.setFetchSize(1000);

        runInstanceType
                .createInstance(serviceManager, this.runningWorkload, values)
                .setMaxThreads(maxThreads)
                .execute(tps, (customData, threadData) -> {
                    int id = LoadGeneratorUtils.getInt(1,ROWS_TO_PRELOAD);;
                    runPointReadOrders(id);

                    if(runInserts){
                        runInserts();
                    }
                });
    }

    private void runPointReadOrders(int id){
        String query = POINT_SELECT_QUERY_ORDERS;
        jdbcTemplate.query(query, new Object[] {id}, new int[] {Types.INTEGER},
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
//                                   System.out.printf("id=%s, col1='%s', col2=%s \n",
//                                            rs.getString("id"),
//                                            rs.getString("total"),
//                                            rs.getString("products")
//                                    );
                    }
                });
    }


    /**
     * Executes the 'runInserts' method, which uses a RetryTemplate for handling retry operations.
     * Generates a unique UUID for tracking retry attempts in logs. The UUID is for logging purposes only
     * and is not stored in the database. Checks for retry occurrence and logs relevant information if applicable.
     * Performs a jdbcTemplate update to insert a record into the database table 'ORDERS'.
     *
     * Note: Ensure that the 'INSERT_RECORD_ORDERS' query, as well as the jdbcTemplate, are appropriately configured
     * for your use case.
     */
    private void runInserts(){
        UUID uuid = LoadGeneratorUtils.getUUID();
        // This UUID is not stored in database. It is for logging purpose only to track the retry operation
        System.out.println("@@@@ACTEST ===>>> test:["+uuid+"]");
        retryTemplate.execute(context -> {
            // Check if retry is happening
            if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
                System.out.println("@@@@@@@@@@@ACTEST ===>>> Retry is happening:["+RetrySynchronizationManager.getContext().getRetryCount()+"] test:["+uuid+"]");
            }
            jdbcTemplate.update(INSERT_RECORD_ORDERS,
                    LoadGeneratorUtils.getDouble(1.00,1000.00),
                    LoadGeneratorUtils.getText(10,40)
            );
            return null;
        });
    }

    /**
     * Executes the 'runUpdates' method, which is annotated with @Transactional for ensuring atomicity.
     * Generates random IDs and update values for tracking purposes and updating records.
     * Logs the details of the update operation including IDs and values.
     * Utilizes a RetryTemplate for handling retry operations, with additional logging for retry occurrences.
     * Performs two jdbcTemplate updates to modify records in the 'ORDERS' table based on randomly generated IDs and values.
     */
    @Transactional
    public void runUpdates() {
        int id1 = LoadGeneratorUtils.getInt(1, 1000000);
        int id2 = LoadGeneratorUtils.getInt(1, 1000000);
        double updateVal1 = LoadGeneratorUtils.getDouble(1.00, 1000.00);
        double updateVal2 = LoadGeneratorUtils.getDouble(1.00, 1000.00);
        String str = "id:[" + id1 + "]=[" + updateVal1 + "] id:[" + id2 + "]=[" + updateVal2 + "]";
        System.out.println("@@@@ACTEST ===>>> Going to UPDATE " + str);
        retryTemplate.execute(context -> {
            // Check if retry is happening
            if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
                System.out.println("@@@@@@@@@@@ACTEST ===>>> RETRY IS HAPPENING:[" + RetrySynchronizationManager.getContext().getRetryCount() + "] " + str);
            }
            jdbcTemplate.update(UPDATE_RECORD_ORDERS,
                    updateVal1,
                    id1
            );

            jdbcTemplate.update(UPDATE_RECORD_ORDERS,
                    updateVal2,
                    id2
            );
            return null;
        });

    }
}
