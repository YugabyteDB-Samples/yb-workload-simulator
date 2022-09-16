package com.yugabyte.simulation.service;

import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.FixedStepsWorkloadType;
import com.yugabyte.simulation.workload.FixedStepsWorkloadType.FixedStepWorkloadInstance;
import com.yugabyte.simulation.workload.FixedTargetWorkloadType;
import com.yugabyte.simulation.workload.FixedTargetWorkloadType.FixedTargetWorkloadInstance;
import com.yugabyte.simulation.workload.Step;
import com.yugabyte.simulation.workload.ThroughputWorkloadType;
import com.yugabyte.simulation.workload.ThroughputWorkloadType.ThroughputWorkloadInstance;

public class WorkloadInvoker {

	private ServiceManager serviceManager;
	private ParamValue[] params;
	private WorkloadDesc workload;
	public WorkloadInvoker(ServiceManager serviceManager, WorkloadDesc workload, ParamValue[] params) {
		this.serviceManager = serviceManager;
		this.workload = workload;
		this.params = params;
	}
	
	public FixedStepWorkloadInstance newFixedStepsInstance(Step ... steps) {
		return new FixedStepsWorkloadType(steps).createInstance(this.serviceManager);
	}
	
	public FixedTargetWorkloadInstance newFixedTargetInstance() {
		return new FixedTargetWorkloadType().createInstance(this.serviceManager, this.workload, this.params);
	}
	
	public ThroughputWorkloadInstance newThroughputWorkloadInstance() {
		return new ThroughputWorkloadType().createInstance(serviceManager, this.workload, this.params);
	}
}
