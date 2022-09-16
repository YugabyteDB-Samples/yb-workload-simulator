package com.yugabyte.simulation.workload;

import com.yugabyte.simulation.workload.FixedStepsWorkloadType.ExecuteStep;

public class Step{
	String stepName;
	ExecuteStep step;
	public Step(String stepName, ExecuteStep step) {
		this.step = step;
		this.stepName = stepName;
	}
}