package com.yugabyte.simulation.services;

public interface Timer {
	public Timer start();
	public Timer timeSubPortion(String description);
	public long end(ExecutionStatus status, int workloadOrdinal);
}
