package com.yugabyte.simulation.workload;


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class WorkloadSimulationBase {
	@Autowired
	private Environment env;

	public static class Workload {
		private final String workloadId;
		private final long startTime;
		public Workload(WorkloadType type) {
			this.startTime = System.nanoTime();
			this.workloadId = type.getTypeName() + "_" + this.startTime;
		}
		
		public String getWorkloadId() {
			return workloadId;
		}
		
		public void terminate() {
			
		}
		
		public void start() {
			
		}
		
	}

	protected static  CqlSession cassandra_session = null;
	/**
	 * We create one shared Cassandra client. This is a non-synchronized method, so multiple threads
	 * can call it without any performance penalty. If there is no client, a synchronized thread is
	 * created so that exactly only one thread will create a client. If there is a pre-existing
	 * client, we just return it.
	 * @return a Cassandra Session object.
	 */
	protected synchronized CqlSession getCassandraClient() {
		if (cassandra_session == null) {
			String userId = env.getProperty("spring.data.cassandra.userid");
			String password = env.getProperty("spring.data.cassandra.password");
			int port = Integer.parseInt(env.getProperty("spring.data.cassandra.port"));
			String datacenter = env.getProperty("spring.data.cassandra.local-datacenter");
			String contactPoints = env.getProperty("spring.data.cassandra.contact-points");

			CqlSessionBuilder builder = CqlSession.builder();
			builder.addContactPoints(Arrays.stream(contactPoints.split(","))
					.map(s -> new InetSocketAddress(s, port))
					.collect(Collectors.toList()))
					.withLocalDatacenter(datacenter);
			if(userId != null){
				builder.withAuthCredentials(userId,password);
			}

			cassandra_session = builder.build();
		}
		return cassandra_session;
	}


}
