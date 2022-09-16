package com.yugabyte.simulation.service;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.FixedStepsWorkloadType;
import com.yugabyte.simulation.workload.FixedTargetWorkloadType;
import com.yugabyte.simulation.workload.Step;
import com.yugabyte.simulation.workload.WorkloadSimulationBase;

@Repository
public class PitrSqlDemoWorkload extends WorkloadSimulationBase implements WorkloadSimulation {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private ServiceManager serviceManager;
	
	@Override
	public String getName() {
		return "SQL PITR Demo";
	}

	private static final String CREATE_TABLE = 
			"create table if not exists pitr_demo ("
			+ "id bigint not null,"
			+ "data varchar(40),"
			+ "create_dt_str varchar(40) not null default to_char(now(), 'DD-MON-YYYY HH:MI:SS PM'),"
			+ "create_dt timestamp not null default now(), "
			+ "constraint pitr_demo_id primary key (id)"
			+ ");";
			
	private final String DROP_TABLE = "drop table if exists pitr_demo;";
	
	private final String INSERT = "INSERT into pitr_demo(id, data) values (?, ?);";
	
	private enum WorkloadType {
		CREATE_TABLES, 
		RUN_SIMULATION,
	}		
	
	private final FixedStepsWorkloadType createTablesWorkloadType;
	private final FixedTargetWorkloadType runInstanceType;
	
	public PitrSqlDemoWorkload() {
		this.createTablesWorkloadType = new FixedStepsWorkloadType(
				new Step("Drop Table", (a,b) -> {
					jdbcTemplate.execute(DROP_TABLE);	
				}),
				new Step("Create Table", (a,b) -> {
					jdbcTemplate.execute(CREATE_TABLE);	
				})
		);
				
		this.runInstanceType = new FixedTargetWorkloadType();
	}
	
	private WorkloadDesc createTablesWorkload = new WorkloadDesc(
			WorkloadType.CREATE_TABLES.toString(),
			"Create Tables", 
			"Create the table. If the table already exists it will be dropped"
		);
	
	private WorkloadDesc runningWorkload = new WorkloadDesc(
			WorkloadType.RUN_SIMULATION.toString(),
			"Simulation",
			"Run a simulation of a simple table",
			new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000),
			new WorkloadParamDesc("Delay", 0, 1000000, 0)
		);
	
	@Override
	public List<WorkloadDesc> getWorkloads() {
		return Arrays.asList(
			createTablesWorkload, runningWorkload
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
		createTablesWorkloadType.createInstance(serviceManager).execute();
	}
	
	private void runSimulation(int target, int delay) {
		runInstanceType
			.createInstance(serviceManager)
			.setDelayBetweenInvocations(delay)
			.setCustomData(Integer.valueOf(0))
			.execute(1, target, (customData, threadData) -> {
				int value = threadData == null? (Integer)customData : (Integer)threadData;
				jdbcTemplate.update(INSERT, new Object[] {value, LoadGeneratorUtils.getName()}, new int[] {Types.INTEGER, Types.VARCHAR});
				return Integer.valueOf(value+1);
			});
	}
}
