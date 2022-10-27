package com.yugabyte.simulation.service;

import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.util.GeneralUtility;
import com.yugabyte.simulation.workload.Step;
import com.yugabyte.simulation.workload.WorkloadSimulationBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class XclusterWorkload extends WorkloadSimulationBase implements WorkloadSimulation {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public String getName() {
		return "New XCluster Workload";
	}
	
	// We need to call a Bean from a Bean so the AOP works.
	@Autowired
	@Lazy private XclusterWorkload self;

	//Atomicity Testing
	private static final String CREATE_ACCOUNT_BALANCE = "create table if not exists account_balance(\n" +
			"   id uuid,\n" +
			"   name text,\n" +
			"   salary int,\n" +
			"   primary key (id)\n" +
			");";
    //Global Ordering Testing
	private static final String CREATE_ORDERING_TEST = "create table if not exists ordering_test(\n" +
			"   id uuid,\n" +
			"   primary key (id)\n" +
			");";

	//JSOB, FK, etc Testing
	private static final String CREATE_CUSTOMER = "create table if not exists customers(\n" +
			"   customer_id uuid,\n" +
			"   customer_state text,\n" +
			"   primary key (customer_id)\n" +
			");";

	private static final String CREATE_ORDER = "create table if not exists orders" +
			"(\n" +
			"   customer_id uuid,\n" +
			"   order_id int,\n" +
			"   order_details jsonb,\n" +
			"   order_qty text,\n" +
			"   primary key (order_id),\n" +
			"   CONSTRAINT fk_customer FOREIGN KEY(customer_id) REFERENCES customers(customer_id))";

	/*
	create table orders(customer_id uuid,
      order_id int primary key,
      order_details jsonb,
      order_qty text,
      CONSTRAINT fk_customer FOREIGN KEY(customer_id) REFERENCES customer(customer_id));
      create index order_index on orders (order_id, customer_id) include(order_qty);
      create index customer_index on customers (customer_id) where customer_state='TX';
	 */
	//indexes
	private final String CREATE_INDEX = "create index name_sal on account_balance (name, salary);";
	private final String CREATE_COVERING_INDEX = "create index order_index on orders (order_id, customer_id) include(order_qty);";
	private final String CREATE_PARTIAL_INDEX = "create index customer_index on customers (customer_id) where customer_state='TX';";

	private static final String DROP_account_balance = "drop table if exists account_balance cascade;";

	//Data Loading
	private static final String INSERT_account_balance = "insert into account_balance(id, name, salary) values(?,?,?);";
	private static final String INSERT_ordering_test = "insert into ordering_test(id) values(?);";
	private static final String INSERT_customer = "insert into customers(customer_id, customer_state) values(?,?)";
	private static final String INSERT_order = "insert into orders(customer_id, order_id, order_details, order_qty) values(?,?,to_jsonb(?),?)";

	//Atomicity
	/* private final String UPDATE_account_balance = "Begin transaction; UPDATE account_balance SET salary = salary - 50 WHERE name = 'Bill'; \n" +
			"UPDATE account_balance SET salary = salary - 50 WHERE name = 'Kannan'; \n" +
			"UPDATE account_balance SET salary = salary + 100 WHERE name = 'Karthik'; \n" +
			"UPDATE account_balance SET salary = salary - 50 WHERE name = 'N8VZRGVH4F'; \n" +
			"UPDATE account_balance SET salary = salary - 50 WHERE name = 'IRDU9UYEVG'; \n" +
			"UPDATE account_balance SET salary = salary + 100 WHERE name = 'YAQJT5YFQD'; \n" +
			"UPDATE account_balance SET salary = salary - 50 WHERE name = 'W7J05QEVPX'; \n" +
			"UPDATE account_balance SET salary = salary - 50 WHERE name = 'D0L7KZ0CID'; \n" +
			"UPDATE account_balance SET salary = salary + 50 WHERE name = 'KW3CHU1UL9'; \n" +
			"UPDATE account_balance SET salary = salary + 50 WHERE name = 'M7YIRNOOKN'; COMMIT;";
*/
	 private final String UPDATE_account_balance = "Begin transaction; \n " +
			"UPDATE account_balance set salary = salary + 50 where id in (select id from account_balance  order by random() limit 10); \n" +
			"UPDATE account_balance set salary = salary - 50 where id in (select id from account_balance  order by random() limit 10); \n" +
			"UPDATE account_balance SET salary = salary - 20 where id in (select id from account_balance  order by random() limit 10); \n" +
			"UPDATE account_balance SET salary = salary + 20 where id in (select id from account_balance  order by random() limit 10); \n" +
			"COMMIT;";

	// Verification queries to be executed from target side
	// sum(salary) = 1,000,000 or raise exception
	//private final String SELECT_validation_query = "select sum(salary) from account_balance where name in ('Bill','Kannan','Karthik', 'N8VZRGVH4F', 'IRDU9UYEVG','YAQJT5YFQD','W7J05QEVPX','D0L7KZ0CID','KW3CHU1UL9','M7YIRNOOKN');";
	private final String SELECT_validation_query = "select sum(salary) from account_balance;";

	//global ordering test
	private final String SELECT_order_verify = "insert into order_verify_result select max(id), sum(id), (max(id)*(max(id)+1))/2 AS expected_sum from ordering_test";
	private final String SELECT_INSERT_order_verify = "insert into order_verify_result select max(id), sum(id), (max(id)*(max(id)+1))/2 AS expected_sum from ordering_test";
	private final String SELECT_INSERT_validation_query = "insert into account_sum select sum(salary) from account_balance;";

	private static final int ROWS_TO_PRELOAD = 10000;
	
	private enum WorkloadType {
		CREATE_TABLES, 
		SEED_DATA_ATOMICITY,
		SEED_DATA_CUSTOMERS,
		SIMULATE_ATOMICITY,
		VALIDATE_ATOMICITY,
		SIMULATE_GLOBAL_ORDERING,
		VALIDATE_GLOBAL_ORDERING
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
							new Step("Drop Table", (a,b) -> jdbcTemplate.execute(CREATE_ORDERING_TEST)),
							new Step("Create Table", (a,b) -> jdbcTemplate.execute(CREATE_ACCOUNT_BALANCE)),
                            new Step("Create Table", (a,b) -> jdbcTemplate.execute(CREATE_CUSTOMER)),
		                    new Step("Create Table", (a,b) -> jdbcTemplate.execute(CREATE_ORDER))
						 //   new Step("Create Index", (a,b) -> jdbcTemplate.execute(CREATE_COVERING_INDEX)),
					     //   new Step("Create Index", (a,b) -> jdbcTemplate.execute(CREATE_PARTIAL_INDEX)),
						//	new Step("Create Index", (a,b) -> jdbcTemplate.execute(CREATE_INDEX))
						)
						.execute();
					}),

				new WorkloadDesc(
						WorkloadType.SEED_DATA_ATOMICITY.toString(),
						"Seed - Atomicity",
						"Create and load sample data",
						new WorkloadParamDesc("Number of records", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("Threads", 1, 500, 32)
					)
					.onInvoke((runner, params) -> {
						jdbcTemplate.setFetchSize(1000);

						runner.newFixedTargetInstance()
							//.setCustomData(currentValue)
							.execute(params.asInt(1), params.asInt(0),
									(customData, threadData) -> {
								insertRecordAtomicity();
								return null;
							});
					}),

				new WorkloadDesc(
						WorkloadType.SEED_DATA_CUSTOMERS.toString(),
						"Seed - Customers-Orders",
						"Create and load sample data",
						new WorkloadParamDesc("Number of records", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("Threads", 1, 500, 32)
				)
						.onInvoke((runner, params) -> {
							jdbcTemplate.setFetchSize(1000);

							runner.newFixedTargetInstance()
									//.setCustomData(currentValue)
									.execute(params.asInt(1), params.asInt(0),
											(customData, threadData) -> {
												insertCustomerOrder();
												return null;
											});
						}),

				new WorkloadDesc(
						WorkloadType.SIMULATE_ATOMICITY.toString(),
						"Simulation - Atomicity",
						"Run a series of update statements to move money around",
						//	new WorkloadParamDesc("TPS", 1, Integer.MAX_VALUE, 1000),
						//	new WorkloadParamDesc("MaxThreads", 1, 500, 32)
						new WorkloadParamDesc("TPS", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("MaxThreads", 1, 500, 32)
				)

						.onInvoke((runner, params) -> {
							jdbcTemplate.setFetchSize(1000);

							runner.newThroughputWorkloadInstance()
									.setMaxThreads(params.asInt(1))
									.execute(params.asInt(0), (customData, threadData) -> {
										//runQueryNoTxn();
										self.runTransactionalUpdatesAtomicity();
										//jdbcTemplate.execute(UPDATE_account_balance);
									});

						}),

				new WorkloadDesc(
						WorkloadType.SIMULATE_GLOBAL_ORDERING.toString(),
						"Simulation - Global Ordering",
						"Create and load sample data",
						new WorkloadParamDesc("Number of records", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("Threads", 1, 500, 32)
				)
						.onInvoke((runner, params) -> {
							jdbcTemplate.setFetchSize(1000);

							runner.newFixedTargetInstance()
									//.setCustomData(currentValue)
									.execute(params.asInt(1), params.asInt(0),
											(customData, threadData) -> {
												insertRecordGlobalOrdering();
												return null;
											});
						}),

				new WorkloadDesc(
						WorkloadType.VALIDATE_GLOBAL_ORDERING.toString(),
						"Perform validation - Global Ordering",
						"Generate testing sample data and verify ordering",
						new WorkloadParamDesc("TPS", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("MaxThreads", 1, 500, 32)
				)

						.onInvoke((runner, params) -> {
							jdbcTemplate.setFetchSize(1000);

							runner.newThroughputWorkloadInstance()
									.setMaxThreads(params.asInt(1))
									.execute(params.asInt(0), (customData, threadData) -> {
										//runQueryNoTxn();
										self.runGlobalOrderingTest();
									});

						}),
                //validation: to run from target side to capture % of incorrectness of sum(salary) calculation
				//results is loaded to account_sum: sum(salary) <> 1,000000 vs sum(salary) = 1,000000
				new WorkloadDesc(WorkloadType.VALIDATE_ATOMICITY.toString(),
						"Perform validations - Atomicity",
						"Run validation query to identify incorrect sum(salary)",
						new WorkloadParamDesc("TPS", 1, Integer.MAX_VALUE, 1000),
						new WorkloadParamDesc("MaxThreads", 1, 500, 32)
					)
					.onInvoke((runner, params) -> {
						//final AtomicLong currentValue = new AtomicLong();
						//jdbcTemplate.query("select max(subscription_id) from subscriptions",
					//			(rs) -> { currentValue.set(rs.getLong(1)+1); } );
						runner.newThroughputWorkloadInstance()
							.setMaxThreads(params.asInt(1))
							.execute(params.asInt(0), (customData, threadData) -> {
								// Note: for transactions to work the @Transactional annotation
								// must b present, the method must be public AND it must be called
								// from another bean. Given that this is a bean, we can self call,
								// provided it's an auto injected reference.
								//self.runValidation();
								//jdbcTemplate.execute(SELECT_validation_query);
								jdbcTemplate.execute(SELECT_INSERT_validation_query);
								AtomicInteger rowCnt = new AtomicInteger();
								jdbcTemplate.query("select count(*) from account_balance",
										(rs) -> { rowCnt.set(rs.getInt(1));});
										//(rs) -> { rowCnt.set(rs.getInt(1)+1);});

								jdbcTemplate.query("select sum(salary) from account_balance",
												(rs) -> {
									Integer currentVal = rs.getInt(1);
									if (currentVal != 100000*rowCnt.get()){
										throw new RuntimeException("Sum was " + currentVal + " expected " + (100000*rowCnt.get()) );
									}

								} );

							});
					})

			);
	}

	//Global Ordering - run this from the target cluster
	public void runGlobalOrderingTest(){
		jdbcTemplate.update(SELECT_order_verify);
	}

	//Global Ordering - run this from the source cluster to generate and load sample data
	public void insertRecordGlobalOrdering(){
		jdbcTemplate.update(INSERT_ordering_test,
				LoadGeneratorUtils.getInt(1,30)
		);
	}

	//Atomicity - Move money around: run on source side
	@Transactional
	public void runTransactionalUpdatesAtomicity() {
		jdbcTemplate.update(UPDATE_account_balance);
	}

	//Atomicity - Check money: run on target side
	private void runValidation(){

		jdbcTemplate.query(SELECT_validation_query,
				new RowCallbackHandler() {
					@Override
					public void processRow(ResultSet rs) throws SQLException {
					}
				});

	}
	//Atomicity - Check money: run on source side
	private void insertRecordAtomicity() {
		UUID uuid = LoadGeneratorUtils.getUUID();
		jdbcTemplate.update(INSERT_account_balance,
				uuid,
				LoadGeneratorUtils.getAlphaString(10),
				100000
		);

	}

	//parent-child / index testing
	private void insertCustomerOrder() {
		UUID uuid = LoadGeneratorUtils.getUUID();
		//customer
		jdbcTemplate.update(INSERT_customer,
				uuid,
				LoadGeneratorUtils.getAlphaString(10)
		);

        //generate multiple orders for each customer
		for (int i = 0; i < 5; i++) {
			//order
			jdbcTemplate.update(INSERT_order,
					uuid,
					LoadGeneratorUtils.getInt(1,100000000), //order_id
					GeneralUtility.getRandomJSONString(1),//order_details
					LoadGeneratorUtils.getDouble() //order_qty
			);
			//System.out.println(i);
		}

	}

}
