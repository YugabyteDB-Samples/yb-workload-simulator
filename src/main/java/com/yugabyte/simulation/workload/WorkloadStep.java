package com.yugabyte.simulation.workload;

public class WorkloadStep {
	private final String name;
	private boolean complete = false;
	private long executionTimeInNs = -1;
	public WorkloadStep(String name) {
		this.name = name;
	}
	
	public void complete() {
		this.complete = true;
	}
	
	public void complete(long timeInNs) {
		this.executionTimeInNs = timeInNs;
		this.complete();
	}
	
	public boolean isComplete() {
		return complete;
	}
	
	public String getName() {
		return name;
	}
	
	public long getExecutionTimeInNs() {
		return executionTimeInNs;
	}
}