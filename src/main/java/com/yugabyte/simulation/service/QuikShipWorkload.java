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
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class QuikShipWorkload extends WorkloadSimulationBase implements WorkloadSimulation {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceManager serviceManager;

    @Value("${SPRING_APPLICATION_NAME:}")
    private String applicationName;


    @Override
    public String getName() {
        return "QuikShip"+ ((applicationName != null && !applicationName.equals(""))? " ["+applicationName+"]" : "");
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

    private final String POINT_SELECT_QUERY_ORDERS = "select id, total, products from orders where id = ?;";

    private static final int ROWS_TO_PRELOAD = 10000;

    private enum WorkloadType {
        CREATE_TABLES,
        SEED_DATA,
        RUN_SIMULATION_FIXED_WORKLOAD,
        RUN_SIMULATION,
        RUN_LIKE_QUERY_ON_GENERIC2,
        RUN_LIKE_QUERY_ON_GENERIC3,
        START_NODE,
        STOP_NODE
    }

    private final FixedStepsWorkloadType createTablesWorkloadType;
    private final FixedTargetWorkloadType seedingWorkloadType;
    private final ThroughputWorkloadType runInstanceType;
    private final FixedTargetWorkloadType simulationFixedWorkloadType;

    public QuikShipWorkload() {
        this.createTablesWorkloadType = new FixedStepsWorkloadType(
                new Step("Drop orders", (a,b) -> jdbcTemplate.execute(DROP_ORDERS)),
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
    }

    private WorkloadDesc createTablesWorkload = new WorkloadDesc(
            QuikShipWorkload.WorkloadType.CREATE_TABLES.toString(),
            "Create Tables",
            "Create the database tables. If the table already exists it will be dropped"
    );

    private WorkloadDesc seedingWorkload = new WorkloadDesc(
            QuikShipWorkload.WorkloadType.SEED_DATA.toString(),
            "Seed Data",
            "Load data into the orders table",
            new WorkloadParamDesc("Items to generate:", 1, Integer.MAX_VALUE, 10000),
            new WorkloadParamDesc("Threads", 1, Integer.MAX_VALUE, 32)
    );

    private WorkloadDesc runningWorkload = new WorkloadDesc(
            QuikShipWorkload.WorkloadType.RUN_SIMULATION.toString(),
            "Simulation - TPS",
            "Run a simulation of a reads on orders placed",
            new WorkloadParamDesc("Throughput (tps)", 1, 1000000, 500),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64),
            new WorkloadParamDesc("Include placing of new orders (inserts)", false)
    );

    private WorkloadDesc simulationFixedWorkload = new WorkloadDesc(
            QuikShipWorkload.WorkloadType.RUN_SIMULATION_FIXED_WORKLOAD.toString(),
            "Simulation",
            "Run a simulation of a reads on orders placed",
            new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000000),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64),
            new WorkloadParamDesc("Include placing of new orders (inserts)", false)
    );


    @Override
    public List<WorkloadDesc> getWorkloads() {
        return Arrays.asList(
                createTablesWorkload
                , seedingWorkload
                , simulationFixedWorkload
                , runningWorkload
        );
    }


    @Override
    public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
        QuikShipWorkload.WorkloadType type = QuikShipWorkload.WorkloadType.valueOf(workloadId);
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
                case STOP_NODE:
                    return new InvocationResult("Ok");
                case START_NODE:
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
//                    UUID uuid = LoadGeneratorUtils.getUUID();
//                    jdbcTemplate.update(INSERT_RECORD_GENERIC1,
//                            uuid,
//                            LoadGeneratorUtils.getInt(0, 100),
//                            LoadGeneratorUtils.getInt(20, 300),
//                            LoadGeneratorUtils.getInt(100, 1000),
//                            LoadGeneratorUtils.getInt(0, 1000),
//                            LoadGeneratorUtils.getDouble(),
//                            LoadGeneratorUtils.getDouble(),
//                            LoadGeneratorUtils.getDouble()
//                    );
//                    jdbcTemplate.update(INSERT_RECORD_GENERIC2,
//                            uuid,
//                            LoadGeneratorUtils.getAlphaString(LoadGeneratorUtils.getInt(1,30))
//                    );
//                    jdbcTemplate.update(INSERT_RECORD_GENERIC3,
//                            uuid,
//                            LoadGeneratorUtils.getAlphaString(LoadGeneratorUtils.getInt(1,255)),
//                            LoadGeneratorUtils.getAlphaString(LoadGeneratorUtils.getInt(1,30))
//                    );
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


    private void runInserts(){
        UUID uuid = LoadGeneratorUtils.getUUID();
        jdbcTemplate.update(INSERT_RECORD_ORDERS,
                LoadGeneratorUtils.getDouble(1.00,1000.00),
                LoadGeneratorUtils.getText(10,40)
        );
    }



}
