package com.yugabyte.simulation.workload;

import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.TimerResult;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.exception.MultipleAggregationWorkloadException;
import com.yugabyte.simulation.services.ServiceManager;

public final class AggregationWorkloadType extends WorkloadType {

	public static final String AGGREGATION_WORKLOAD_NAME = "Aggregation Counter";
	public static final String csvHeader = "Start Time,Min Time Us,Average Time Us,Max Time Us,Num Succeeded,Num Failed\n";
	private static final String csvFormat = "%d,%d,%d,%d,%d,%d\n";
	
	private static final AggregationWorkloadInstanceType instance = null; 
	private final class AggregationWorkloadInstanceType extends WorkloadTypeInstance {

		public AggregationWorkloadInstanceType(ServiceManager serviceManager) {
			super(serviceManager);
			if (instance != null) {
				serviceManager.getTimerService().stopTimingWorkload(this);
				throw new MultipleAggregationWorkloadException();
			}
		}

		@Override
		protected String createWorkloadId() {
			return "Aggregation Counter";
		}
		
		@Override
		public WorkloadType getType() {
			return AggregationWorkloadType.this;
		}

		@Override
		public boolean isComplete() {
			return false;
		}
		
		@Override
		public String formatToCsv(TimerResult result) {
			return String.format(csvFormat, result.getStartTimeMs(), result.getMinUs(), result.getAvgUs(),
					result.getMaxUs(), result.getNumSucceeded(), result.getNumFailed());
		}
		
		@Override
		public String getCsvHeader() {
			return csvHeader;
		}
	}
	
	@Override
	public String getTypeName() {
		return AGGREGATION_WORKLOAD_NAME;
	}

	@Override
	public boolean canBeTerminated() {
		return false;
	}
	
	@Override
	public WorkloadTypeInstance createInstance(ServiceManager serviceManager) {
		return new AggregationWorkloadInstanceType(serviceManager);
	}

	@Override
	public WorkloadTypeInstance createInstance(ServiceManager serviceManager, WorkloadDesc workload,
			ParamValue[] params) {
		throw new IllegalAccessError("Method AggregationWorkloadType.createInstance(serviceManager, workload, params) is not implemented by design");
	}
}
