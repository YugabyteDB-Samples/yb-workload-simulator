package com.yugabyte.simulation.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.workload.Step;
import com.yugabyte.simulation.workload.WorkloadSimulationBase;

@Repository
public class NewFormatWorkload extends WorkloadSimulationBase implements WorkloadSimulation {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public String getName() {
		return "New Format Workload";
	}

	private static final String CREATE_TABLE = 
			"create table if not exists newsletter_subscriptions ("
			+ "subscription_id bigint not null, "
			+ "cust_id bigint not null,"
			+ "mpid bigint not null,"
			+ "mcode varchar(11) not null,"
			+ "subscribed_ind smallint not null,"
			+ "opt_in_date timestamp,"
			+ "opt_out_date timestamp,"
			+ "opt_in_source varchar(256),"
			+ "create_dt timestamp not null default now(), "
			+ "mod_dt timestamp not null default now(), "
			+ "constraint newsletter_subscriptions_pk primary key (cust_id, mcode)"
			+ ") split into 1 tablets;";
			
	private final String DROP_TABLE = "drop table if exists newsletter_subscriptions;";
	
	private final String CREATE_INDEX = "create index newsletter_subscriptions_id on newsletter_subscriptions ( cust_id );";

	private final String QUERY = "select SUBSCRIPTION_ID, CUST_ID, MCODE, MPID, SUBSCRIBED_IND, OPT_IN_DATE, OPT_OUT_DATE, OPT_IN_SOURCE from NEWSLETTER_SUBSCRIPTIONS where CUST_ID = ? and SUBSCRIBED_IND = 1 /** SportyApi **/";
	
	private final String INSERT = 
			"insert into newsletter_subscriptions ("
			+ "subscription_id, cust_id, mpid, mcode, subscribed_ind,"
			+ "opt_in_date, opt_out_date, opt_in_source)"
			+ " values "
			+ "(?, ?, ?, ?, ?, ?, ?, ?);";
	
	private enum WorkloadType {
		CREATE_TABLES, 
		SEED_DATA,
		UNBOUNDED_SIMULATION,
		RUN_SIMULATION,
	}		
	
	public List<WorkloadDesc> getWorkloads() {
		return Arrays.asList(
				new WorkloadDesc(
						WorkloadType.CREATE_TABLES.toString(),
						"Create Tables"
					)
					.setDescription("Create the table. If the table already exists it will be dropped")
					.onInvoke((runner, params) -> {
						runner.newFixedStepsInstance(
							new Step("Drop Table", (a,b) -> jdbcTemplate.execute(DROP_TABLE)),	
							new Step("Create Table", (a,b) -> jdbcTemplate.execute(CREATE_TABLE)),	
							new Step("Create Index", (a,b) -> jdbcTemplate.execute(CREATE_INDEX))	
						)
						.execute();
					}),

				new WorkloadDesc(
						WorkloadType.SEED_DATA.toString(),
						"Seed the data",
						"Create sample data",
						new WorkloadParamDesc("Number of records", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("Threads", 1, 500, 32)
					)
					.onInvoke((runner, params) -> {
						jdbcTemplate.setFetchSize(1000);
	
						final AtomicLong currentValue = new AtomicLong();
						jdbcTemplate.query("select max(subscription_id) from newsletter_subscriptions",
								(rs) -> { currentValue.set(rs.getLong(1)+1); } );

						runner.newFixedTargetInstance()
							.setCustomData(currentValue)
							.execute(params.asInt(1), params.asInt(0),
									(customData, threadData) -> {
								insertRecord((AtomicLong)customData);
								return null;
							});
					}),

				new WorkloadDesc(
						WorkloadType.RUN_SIMULATION.toString(),
						"Simulation",
						"Run a simulation of a simple table with finie bounds",
						new WorkloadParamDesc("Invocations", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("Delay", 0, 1000000, 0),
						new WorkloadParamDesc("Threads", 1, 500, 32)
					)
					.onInvoke((runner, params) -> {
						jdbcTemplate.setFetchSize(1000);

						runner.newFixedTargetInstance()
							.setDelayBetweenInvocations(params.asInt(1))
							.execute(params.asInt(2), params.asInt(0), (customData, threadData) -> {
								runQueryNoTxn();
								return null;
							});

					}),
				
				new WorkloadDesc(
						WorkloadType.UNBOUNDED_SIMULATION.toString(),
						"Unbounded Simulation",
						"Run a simulation of a simple table",
						new WorkloadParamDesc("TPS", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("MaxThreads", 1, 500, 32)
					)
					.onInvoke((runner, params) -> {
						jdbcTemplate.setFetchSize(1000);

						runner.newThroughputWorkloadInstance()
							.setMaxThreads(params.asInt(1))
							.execute(params.asInt(0), (customData, threadData) -> {
								runQueryNoTxn();
							});

					})

			);
	}
	
	private void insertRecord(AtomicLong currentCounter) {
		jdbcTemplate.update(INSERT, new Object[] {
			currentCounter.getAndIncrement(),
			LoadGeneratorUtils.getLong(1000, 30_000_00),
			LoadGeneratorUtils.getLong(10, 500),
			LoadGeneratorUtils.getHexString(7),
			LoadGeneratorUtils.getInt(0, 2),
			new Date(),
			new Date(),
			null
		}, new int[] {
			Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.VARCHAR, 
			Types.SMALLINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR
		});
	}
	
	private void runQueryNoTxn() {
		int custNum = ThreadLocalRandom.current().nextInt(1000, 20_000_000);
		jdbcTemplate.query(QUERY, new Object[] {custNum}, new int[] {Types.INTEGER},
			new RowCallbackHandler() {
				@Override
				public void processRow(ResultSet rs) throws SQLException {
				}
			});
	}
}
