package com.yugabyte.simulation.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class GenericCassandraWorkload extends WorkloadSimulationBase implements WorkloadSimulation{
/*    public static void main(String[] args) {
        GenericCassandraWorkload test = new GenericCassandraWorkload();
        CqlSession session = test.getCassandraClient();
        ResultSet rs = session.execute("SELECT * FROM urs.actest;");

        for (Row row : rs) {
            // process the row
            int count = 0;
            System.out.println(row.getString(count++)+","+row.getString(count++));
        }

        session.close();
    }*/

    @Autowired
    private ServiceManager serviceManager;

    @Override
    public String getName() {
        return "Generic YCQL";
    }


    private static final String CREATE_GENERIC1 = "create table if not exists workload_demo.c_generic1(\n" +
            "   pkid uuid primary key,\n" +
            "   col1 varchar\n" +
            ");";

    private static final String CREATE_GENERIC2 = "create table  if not exists generic2(\n" +
            "   pkid uuid,\n" +
            "   rawdatacol varchar(30) ,\n" +
            "   primary key (pkid)\n" +
            ");";

    private static final String CREATE_GENERIC3 = "create table if not exists generic3(\n" +
            "   pkid uuid,\n" +
            "   col1 varchar(255),\n" +
            "   rawdatacol varchar(30),\n" +
            "   primary key (pkid)\n" +
            ");";


    private static final String CREATE_KEYSPACE = "create keyspace if not exists workload_demo;";
    private static final String DROP_GENERIC1 = "drop table if exists  workload_demo.c_generic1;";
    private static final String TRUNCATE_GENERIC1 = "truncate generic1;";
    private static final String DROP_GENERIC2 = "drop table if exists generic2 cascade;";
    private static final String TRUNCATE_GENERIC2 = "truncate generic2;";
    private static final String DROP_GENERIC3 = "drop table if exists generic3 cascade;";
    private static final String TRUNCATE_GENERIC3 = "truncate generic3;";

    // column 8 and 9 in table 1 are timestamps. I will let db populate those.
    private static final String INSERT_RECORD_GENERIC1 = "insert into workload_demo.c_generic1(pkid, col1) values(?,?);";
    private static final String INSERT_RECORD_GENERIC2 = "insert into generic2(pkid, rawdatacol) values(?,?);";
    private static final String INSERT_RECORD_GENERIC3 = "insert into generic3(pkid, col1, rawdatacol) values(?,?,?);";

    private final String POINT_SELECT_QUERY_GENERIC1 = "select pkid,col1 from workload_demo.c_generic1 where pkid = ?;";
    private final String POINT_SELECT_QUERY_GENERIC2 = "select pkid,rawdatacol from generic2 where pkid = ?::uuid;";
    private final String POINT_SELECT_QUERY_GENERIC3 = "select pkid,col1,rawdatacol from generic3 where pkid = ?::uuid;";

    private final String SELECT_QUERY_ON_BINARYCOL_GENERIC2 = "select pkid,rawdatacol from generic2 where rawdatacol like ?::bytea limit 100;";
    private final String SELECT_QUERY_ON_BINARYCOL_GENERIC3 = "select pkid,col1,rawdatacol from generic3 where rawdatacol like ?::bytea limit 100;";

    private static final int ROWS_TO_PRELOAD = 10000;

    private enum WorkloadType {
        CREATE_TABLES,
        SEED_DATA,
        RUN_SIMULATION,
        RUN_LIKE_QUERY_ON_GENERIC2,
        RUN_LIKE_QUERY_ON_GENERIC3
    }

    private final FixedStepsWorkloadType createTablesWorkloadType;
    private final FixedTargetWorkloadType seedingWorkloadType;
    private final ThroughputWorkloadType runInstanceType;

    public GenericCassandraWorkload() {
        this.createTablesWorkloadType = new FixedStepsWorkloadType(
                new Step("Create Keyspace workload_demo", (a,b) -> this.getCassandraClient().execute(CREATE_KEYSPACE)),
                new Step("Drop generic1", (a, b) -> this.getCassandraClient().execute(DROP_GENERIC1)),
                new Step("Create generic1", (a,b) -> this.getCassandraClient().execute(CREATE_GENERIC1))
//                new Step("Drop generic2", (a,b) -> this.getCassandraClient().execute(DROP_GENERIC2)),
//                new Step("Create generic2", (a,b) -> this.getCassandraClient().execute(CREATE_GENERIC2)),
//                new Step("Drop generic3", (a,b) -> this.getCassandraClient().execute(DROP_GENERIC3)),
//                new Step("Create generic3", (a,b) -> this.getCassandraClient().execute(CREATE_GENERIC3))
        );

        this.seedingWorkloadType = new FixedTargetWorkloadType();
        this.runInstanceType = new ThroughputWorkloadType();
    }

    private WorkloadDesc createTablesWorkload = new WorkloadDesc(
            GenericCassandraWorkload.WorkloadType.CREATE_TABLES.toString(),
            "Create Tables",
            "Create the table. If the table already exists it will be dropped"
    );

    private WorkloadDesc seedingWorkload = new WorkloadDesc(
            GenericCassandraWorkload.WorkloadType.SEED_DATA.toString(),
            "Seed Data",
            "Load data into the 3 tables (Latency on charts will show cumulative value for 3 inserts)",
            new WorkloadParamDesc("Items to generate:", 1, Integer.MAX_VALUE, 1000),
            new WorkloadParamDesc("Threads", 1, 500, 32)
    );

    private WorkloadDesc runningWorkload = new WorkloadDesc(
            GenericCassandraWorkload.WorkloadType.RUN_SIMULATION.toString(),
            "Simulation",
            "Run a simulation of a reads from 3 tables (Latency on charts will show cumulative value for 3 selects and 3 inserts)",
            new WorkloadParamDesc("Throughput (tps)", 1, 1000000, 500),
            new WorkloadParamDesc("Max Threads", 1, 500, 64),
            new WorkloadParamDesc("Include new Inserts (to 3 tables)", false)
    );

    @Override
    public List<WorkloadDesc> getWorkloads() {
        return Arrays.asList(
                createTablesWorkload
                , seedingWorkload
                , runningWorkload
        );
    }


    @Override
    public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
        GenericCassandraWorkload.WorkloadType type = GenericCassandraWorkload.WorkloadType.valueOf(workloadId);
        try {
            switch (type) {
                case CREATE_TABLES:
                    this.createTables();
                    return new InvocationResult("Ok");

                case SEED_DATA:
                    this.seedData(values[0].getIntValue(), values[1].getIntValue());
                    return new InvocationResult("Ok");

                case RUN_SIMULATION:
                    this.runSimulation(values[0].getIntValue(), values[1].getIntValue(), values[2].getBoolValue());
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
                    UUID uuid = LoadGeneratorUtils.getUUID();
                    CqlSession session = this.getCassandraClient();
                    PreparedStatement ps = session.prepare(INSERT_RECORD_GENERIC1);
                    //ps.bind(uuid,LoadGeneratorUtils.getName());
                    session.execute(ps.bind(uuid,LoadGeneratorUtils.getName()));

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

    private List<UUID> getQueryList() {
        List<UUID> results = new ArrayList<UUID>(ROWS_TO_PRELOAD);
        CqlSession session = this.getCassandraClient();
        PreparedStatement ps = session.prepare("select pkid from  workload_demo.c_generic1 limit "+ROWS_TO_PRELOAD+";");
        ResultSet rs = session.execute(ps.bind());
        for(Row row : rs){
            results.add(row.getUuid("pkid"));
        }
        return results;
    }


    private void runSimulation(int tps, int maxThreads, boolean runInserts) {
        System.out.println("**** Preloading data...");
        final List<UUID> uuids = getQueryList();
        System.out.println("**** Preloading complete...");

        Random random = ThreadLocalRandom.current();
//        jdbcTemplate.setFetchSize(1000);

        runInstanceType
                .createInstance(serviceManager)
                .setMaxThreads(maxThreads)
                .execute(tps, (customData, threadData) -> {
                    UUID id = uuids.get(random.nextInt(uuids.size()));
                    runPointReadgeneric1(id);
                    runPointReadgeneric2(id);
                    runPointReadgeneric3(id);

                    if(runInserts){
                        runInserts();
                    }
                });
    }

    private void runPointReadgeneric1(UUID id){
        String query = POINT_SELECT_QUERY_GENERIC1;
        //TODO
        CqlSession session = this.getCassandraClient();
        PreparedStatement ps = session.prepare(query);
        ResultSet rs = session.execute(ps.bind(id));
        for (Row row : rs) {
            // process the row
            int count = 0;
            System.out.println(row.getUuid("pkid")+","+row.getString("col1"));
        }
    }

    private void runPointReadgeneric2(UUID id){
        String query = POINT_SELECT_QUERY_GENERIC2;
        //TODO
    }

    private void runPointReadgeneric3(UUID id){
        String query = POINT_SELECT_QUERY_GENERIC3;
        // TODO
    }

    private void runInserts(){
        UUID uuid = LoadGeneratorUtils.getUUID();
        // TODO
    }




}
