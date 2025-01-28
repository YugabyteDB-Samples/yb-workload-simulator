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
public class MultiRegionWorkload  extends WorkloadSimulationBase implements WorkloadSimulation {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceManager serviceManager;

    @Value("${SPRING_APPLICATION_NAME:}")
    private String applicationName;


    @Override
    public String getName() {
        return "MultiRegion"+ ((applicationName != null && !applicationName.equals(""))? " ["+applicationName+"]" : "");
    }

    // DDL is available here: src/main/resources/scripts/multi-region-workload-ddl.sql
    // Please run the DDL - This workload doesn't support creating the schema from UI

    private static final String TRUNCATE_TRANSACTIONS = "truncate table transactions;";

    private static final String INSERT_RECORD_TRANSACTIONS = "insert into transactions(transaction_id,region,account_id,symbol,amount) values(?,?,?,?,?);";

    private final String POINT_SELECT_QUERY_TRANSACTIONS = "select transaction_id,region,account_id,symbol,amount,transaction_time from transactions where transaction_id = ?::uuid and region = ?;";

    private static final int ROWS_TO_PRELOAD = 10000;

    private enum WorkloadType {
        TRUNCATE_TABLES,
        SEED_DATA,
        RUN_SIMULATION_FIXED_WORKLOAD
    }

    private final FixedStepsWorkloadType createTablesWorkloadType;
    private final FixedTargetWorkloadType seedingWorkloadType;
    private final ThroughputWorkloadType runInstanceType;
    private final FixedTargetWorkloadType simulationFixedWorkloadType;

    public MultiRegionWorkload() {
        this.createTablesWorkloadType = new FixedStepsWorkloadType(
                new Step("Truncate transactions table", (a,b) -> jdbcTemplate.execute(TRUNCATE_TRANSACTIONS))
        );

        this.seedingWorkloadType = new FixedTargetWorkloadType();
        this.runInstanceType = new ThroughputWorkloadType();
        this.simulationFixedWorkloadType = new FixedTargetWorkloadType();
    }

    private WorkloadDesc truncateTablesWorkload = new WorkloadDesc(
            MultiRegionWorkload.WorkloadType.TRUNCATE_TABLES.toString(),
            "Truncate Tables",
            "Truncate the transactions table"
    );

    private WorkloadDesc seedingWorkload = new WorkloadDesc(
            MultiRegionWorkload.WorkloadType.SEED_DATA.toString(),
            "Seed Data",
            "Load data into the transactions table",
            new WorkloadParamDesc("Items to generate:", 1, Integer.MAX_VALUE, 10000),
            new WorkloadParamDesc("Threads", 1, Integer.MAX_VALUE, 32),
            new WorkloadParamDesc("Region", 0,new String[]{"us-east-1", "us-east-2"})
    );

    private WorkloadDesc simulationFixedWorkload = new WorkloadDesc(
            MultiRegionWorkload.WorkloadType.RUN_SIMULATION_FIXED_WORKLOAD.toString(),
            "Simulation",
            "Run a simulation of a reads on transactions",
            new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000000),
            new WorkloadParamDesc("Max Threads", 1, Integer.MAX_VALUE, 64),
            new WorkloadParamDesc("Region", 0,new String[]{"us-east-1", "us-east-2"}),
            new WorkloadParamDesc("Include Insert data", false)
    );


    @Override
    public List<WorkloadDesc> getWorkloads() {
        return Arrays.asList(
                truncateTablesWorkload
                , seedingWorkload
                , simulationFixedWorkload
        );
    }


    @Override
    public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
        MultiRegionWorkload.WorkloadType type = MultiRegionWorkload.WorkloadType.valueOf(workloadId);
        try {
            switch (type) {
                case TRUNCATE_TABLES:
                    this.createTables();
                    return new InvocationResult("Ok");
                case SEED_DATA:
                    this.seedData(values[0].getIntValue(), values[1].getIntValue(), values[2].getStringValue());
                    return new InvocationResult("Ok");
                case RUN_SIMULATION_FIXED_WORKLOAD:
                    this.runSimulationFixedWorkload(values);
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

    private void seedData(int numberToGenerate, int threads, String region) {
        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(threads, numberToGenerate, (customData, threadData) -> {
                    runInserts(region);
                    return threadData;
                });
    }




    private void runSimulationFixedWorkload(ParamValue[] values) {
        int numOfInvocations = values[0].getIntValue();
        int maxThreads = values[1].getIntValue();
        String region = values[2].getStringValue();
        boolean runInserts = values[3].getBoolValue();

        System.out.println("**** Preloading data...");
        final List<UUID> uuids = getQueryList(region);
        for (UUID item: uuids){
            System.out.println(item.toString());
        }
        System.out.println("**** Preloading complete...");
        Random random = ThreadLocalRandom.current();

        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(maxThreads, numOfInvocations, (customData, threadData) -> {
                    UUID transactionId = uuids.get(random.nextInt(uuids.size()));
                    runPointReadOrders(transactionId, region);
                    if(runInserts){
                        runInserts(region);
                    }
                    return threadData;
                });
    }



    private void runPointReadOrders(UUID transactionId, String region){
        String query = POINT_SELECT_QUERY_TRANSACTIONS;
        jdbcTemplate.query(query, new Object[] {transactionId, region}, new int[] {Types.VARCHAR,Types.VARCHAR},
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


    // insert into transactions(transaction_id,region,account_id,symbol,amount) values(?,?,?,?,?);
    private void runInserts(String region){
        UUID uuid = LoadGeneratorUtils.getUUID();
        jdbcTemplate.update(INSERT_RECORD_TRANSACTIONS,
                LoadGeneratorUtils.getUUID(),
                region,
                LoadGeneratorUtils.getInt(1,10000000),
                LoadGeneratorUtils.getAlphaString(3),
                LoadGeneratorUtils.getDouble(1.00,1000.00)
        );
    }


    private List<UUID> getQueryList(String region) {
        List<UUID> results = new ArrayList<UUID>(ROWS_TO_PRELOAD);
        int numOfRanges = 64;
        int limit = ROWS_TO_PRELOAD/numOfRanges;
        int runningHashCodeVal = 0;
        StringBuffer sbQuery = new StringBuffer();

        while(runningHashCodeVal < 65536){
            if(runningHashCodeVal != 0){
                sbQuery.append(" UNION ALL ");
            }
            int nextHashVal = runningHashCodeVal + 1024;
            sbQuery.append(" (SELECT transaction_id FROM transactions where yb_hash_code(transaction_id) >= "+runningHashCodeVal+" and yb_hash_code(transaction_id) < "+nextHashVal+" and region = '"+region+"' LIMIT "+limit+") ");
            runningHashCodeVal = nextHashVal;
        }



        jdbcTemplate.setMaxRows(ROWS_TO_PRELOAD);
        jdbcTemplate.setFetchSize(ROWS_TO_PRELOAD);
        System.out.println("query:"+sbQuery.toString());
        jdbcTemplate.query(sbQuery.toString(),
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        UUID value = (UUID)rs.getObject(1);
                        results.add(value);
                    }
                });

//        System.out.println("list of pkids:");
//        for(UUID pkId: results){
//            System.out.print(pkId.toString()+",");
//        }

        return results;
    }


}
