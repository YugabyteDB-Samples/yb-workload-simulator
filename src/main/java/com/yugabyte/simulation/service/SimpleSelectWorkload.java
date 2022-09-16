package com.yugabyte.simulation.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.FixedStepsWorkloadType;
import com.yugabyte.simulation.workload.FixedTargetWorkloadType;
import com.yugabyte.simulation.workload.Step;
import com.yugabyte.simulation.workload.ThroughputWorkloadType;
import com.yugabyte.simulation.workload.WorkloadSimulationBase;

@Repository
public class SimpleSelectWorkload extends WorkloadSimulationBase implements WorkloadSimulation {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired 
	private ServiceManager serviceManager;
	
	@Override
	public String getName() {
		return "Simple Select";
	}

	private static final String CREATE_TABLE =
			"create table if not exists vulgar_words ("
			+ "id uuid not null, "
			+ "created timestamp not null default now(), "
			+ "word_name varchar(30) not null, "
			+ "active_ind boolean not null default true,"
			+ "constraint vulgar_words_pk primary key (id)"
			+ ") split into 1 tablets;";
			
	private final String DROP_TABLE = "drop table if exists vulgar_words;";
	
	private final String CREATE_INDEX = "create index active_idx on vulgar_words ( active_ind ) include (word_name) where active_ind = true;";

	private final String INSERT_RECORD = "insert into vulgar_words("
			+ "id, word_name, active_ind)"
			+ " values (?, ?, ?);";
	
	private final String QUERY = "select word_name from vulgar_words where active_ind = true;";
	
	private enum WorkloadType {
		CREATE_TABLES, 
		SEED_DATA,
		RUN_SIMULATION,
	}		
	
//	private static final String DROP_TABLE_STEP = "Drop Table";
//	private static final String CREATE_TABLE_STEP = "Create Table";

	private final FixedStepsWorkloadType createTablesWorkloadType;
	private final FixedTargetWorkloadType seedingWorkloadType;
	private final ThroughputWorkloadType runInstanceType;
	
	public SimpleSelectWorkload() {
//		this.createTablesWorkloadType = new FixedStepsWorkloadType(
//				DROP_TABLE_STEP,
//				CREATE_TABLE_STEP);
		
		this.createTablesWorkloadType = new FixedStepsWorkloadType(
				new Step("Pause 1", (a,b) -> { try { Thread.sleep(5000);} catch (Exception e) {} }),
				new Step("Drop Table", (a,b) -> jdbcTemplate.execute(DROP_TABLE)),
				new Step("Pause 2", (a,b) -> { try { Thread.sleep(2000);} catch (Exception e) {} }),
				new Step("Create Table", (a,b) -> jdbcTemplate.execute(CREATE_TABLE)),
				new Step("Create Index", (a,b) -> jdbcTemplate.execute(CREATE_INDEX)),
				new Step("Pause 2", (a,b) -> { try { Thread.sleep(3000);} catch (Exception e) {} })
		);
				
		this.seedingWorkloadType = new FixedTargetWorkloadType();
		this.runInstanceType = new ThroughputWorkloadType();
	}
	
	private WorkloadDesc createTablesWorkload = new WorkloadDesc(
			WorkloadType.CREATE_TABLES.toString(),
			"Create Tables", 
			"Create the table. If the table already exists it will be dropped"
		);
	
	private WorkloadDesc seedingWorkload = new WorkloadDesc(
			WorkloadType.SEED_DATA.toString(),
			"Seed Data",
			"Load data into the table",
			new WorkloadParamDesc("Items to generate:", 1, Integer.MAX_VALUE, 1000),
			new WorkloadParamDesc("Threads", 1, 500, 32)
		);
			
	private WorkloadDesc runningWorkload = new WorkloadDesc(
			WorkloadType.RUN_SIMULATION.toString(),
			"Simulation",
			"Run a simulation of a simple table",
			new WorkloadParamDesc("Throughput (tps)", 1, 1000000, 500),
			new WorkloadParamDesc("Max Threads", 1, 500, 64)
		);
	
	@Override
	public List<WorkloadDesc> getWorkloads() {
		return Arrays.asList(
			createTablesWorkload, seedingWorkload, runningWorkload
		);
	}

	
	@Override
	public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
		WorkloadType type = WorkloadType.valueOf(workloadId);
		try {
			switch (type) {
			case CREATE_TABLES:
				this.createTables();
				return new InvocationResult("Ok");
			
			case SEED_DATA:
				this.seedData(values[0].getIntValue(), values[1].getIntValue());
				return new InvocationResult("Ok");
				
			case RUN_SIMULATION:
				this.runSimulation(values[0].getIntValue(), values[1].getIntValue());
				return new InvocationResult("Ok");

			}
			throw new IllegalArgumentException("Unknown workload "+ workloadId);
		}
		catch (Exception e) {
			return new InvocationResult(e);
		}
	}

	private void createTables() {
//		FixedStepsWorkloadType jobType = createTablesWorkloadType;
//		FixedStepWorkloadInstance workload = jobType.createInstance(timerService);
//		workloadManager.registerWorkloadInstance(workload);
//		workload.execute((stepNum, stepName) -> {
//			switch (stepName) {
//			case DROP_TABLE_STEP:
//				jdbcTemplate.execute(DROP_TABLE);
//				break;
//			case CREATE_TABLE_STEP:
//				jdbcTemplate.execute(CREATE_TABLE);
//				break;
//			}
//		});
		createTablesWorkloadType.createInstance(serviceManager).execute();
	}
	
	private void seedData(int numberToGenerate, int threads) {
		seedingWorkloadType
			.createInstance(serviceManager)
			.execute(threads, numberToGenerate, (customData, threadData) -> {
				UUID uuid = LoadGeneratorUtils.getUUID();
				String name = LoadGeneratorUtils.getName();
				boolean active = LoadGeneratorUtils.getInt(0, 100000) == 0;
				
				jdbcTemplate.update(INSERT_RECORD, uuid, name, active);
				return threadData;
			});
	}

	private void runSimulation(int tps, int maxThreads) {
		jdbcTemplate.setFetchSize(1000);

		runInstanceType
			.createInstance(serviceManager)
			.setMaxThreads(maxThreads)
			.execute(tps, (customData, threadData) -> {
				String query = QUERY;
				jdbcTemplate.query(query,
					new RowCallbackHandler() {
						@Override
						public void processRow(ResultSet rs) throws SQLException {
//							System.out.printf("id=%s, word='%s', active=%b\n", 
//									rs.getString("id"),
//									rs.getString("word_name"),
//									rs.getInt("active_ind"));
						}
					});
			});
	}
}
