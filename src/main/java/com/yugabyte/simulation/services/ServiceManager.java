package com.yugabyte.simulation.services;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yugabyte.simulation.workload.AggregationWorkloadType;
import com.yugabyte.simulation.workload.WorkloadManager;

// This class contains references to all the major services the workloads
// require. These are all autowired, but the actual services themselves are
// not beans and so cannot autowire their dependencies. This class allows them
// just to have one class injected now many.
@Service
public class ServiceManager {
	@Autowired
	private TimerService timerService;
	
	@Autowired
	private WorkloadManager workloadManager;
	
	@Autowired
	private LoggingFileManager loggingFileManager;
	
	private boolean headless = false;
	
	@PostConstruct
	private void createAggregationWorkload() {
		// This will self-register
		new AggregationWorkloadType().createInstance(this);
	}


	public TimerService getTimerService() {
		return timerService;
	}
	
	public WorkloadManager getWorkloadManager() {
		return workloadManager;
	}
	
	public LoggingFileManager getLoggingFileManager() {
		return loggingFileManager;
	}
	
	public void setHeadless(boolean headless) {
		this.headless = headless;
	}
	
	public boolean isHeadless() {
		return headless;
	}
}
