package com.yugabyte.simulation.dao;

import java.util.ArrayList;
import java.util.List;

import com.yugabyte.simulation.workload.WorkloadTypeInstance;

public class WorkloadResult {
	private final String workloadId;
	private final String workloadTypeName;
	private final boolean canBeTerminated;
	private final boolean isTerminated;
	private final List<TimerResult> results;
	private final long startTime;
	private final long endTime;
	private final String status;
	private final String description;
	
	public WorkloadResult(long fromTime, WorkloadTypeInstance instance) {
		this.workloadId = instance.getWorkloadId();
		this.workloadTypeName = instance.getType().getTypeName();
		this.canBeTerminated = instance.getType().canBeTerminated();
		this.isTerminated = instance.isTerminated();
		this.startTime = instance.getStartTime();
		this.endTime = instance.getEndTime();
		this.status = instance.getStatus().toString();
		if (fromTime < Long.MAX_VALUE) {
			this.results = instance.getResults(fromTime);
		}
		else {
			this.results = new ArrayList<TimerResult>();
		}
		this.description = instance.getDescription();
	}

	public String getWorkloadId() {
		return workloadId;
	}

	public String getWorkloadTypeName() {
		return workloadTypeName;
	}

	public boolean isCanBeTerminated() {
		return canBeTerminated;
	}

	public boolean isTerminated() {
		return isTerminated;
	}

	public long getStartTime() {
		return startTime;
	}
	
	public long getEndTime() {
		return endTime;
	}
	
	public String getStatus() {
		return status;
	}
	
	public List<TimerResult> getResults() {
		return results;
	}
	
	public String getDescription() {
		return description;
	}
}
