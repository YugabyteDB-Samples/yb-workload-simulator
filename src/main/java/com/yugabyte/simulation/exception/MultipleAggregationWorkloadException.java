package com.yugabyte.simulation.exception;

public class MultipleAggregationWorkloadException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3822136629219660978L;

	public MultipleAggregationWorkloadException() {
		super("Attempt to create multiple Aggregation Workloads");
	}
}
