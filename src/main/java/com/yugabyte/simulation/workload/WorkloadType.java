package com.yugabyte.simulation.workload;

import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.FixedTargetWorkloadType.FixedTargetWorkloadInstance;

public abstract class WorkloadType {
	public abstract String getTypeName();
	public abstract WorkloadTypeInstance createInstance(ServiceManager serviceManager, WorkloadDesc workload, ParamValue[] params);
	public abstract WorkloadTypeInstance createInstance(ServiceManager serviceManager);
	public boolean canBeTerminated() {
		return true;
	}
}