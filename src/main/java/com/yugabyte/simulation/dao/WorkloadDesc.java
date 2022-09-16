package com.yugabyte.simulation.dao;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yugabyte.simulation.service.WorkloadInvoker;
import com.yugabyte.simulation.services.ServiceManager;

public class WorkloadDesc {
	
	public interface Invoker {
		void invoke(WorkloadInvoker workloadInvoker, ParamHolder params);
	}
	private final String workloadId;
	private final String name;
	private String description;
	private final List<WorkloadParamDesc> params;
	
	@JsonIgnore
	private transient Invoker invoker;
	
	
	public WorkloadDesc(String workloadId, String name, String description, WorkloadParamDesc ... params) {
		super();
		this.workloadId = workloadId;
		this.name = name;
		this.description = description;
		this.params = Arrays.asList(params);
	}

	public WorkloadDesc(String workloadId, String name, WorkloadParamDesc ... params) {
		super();
		this.workloadId = workloadId;
		this.name = name;
		this.params = Arrays.asList(params);
	}

	public String getWorkloadId() {
		return workloadId;
	}
	
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public List<WorkloadParamDesc> getParams() {
		return params;
	}
	
	public WorkloadDesc setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public WorkloadDesc onInvoke(Invoker invoker) {
		this.invoker = invoker;
		return this;
	}
	
	public void invoke(ServiceManager serviceManager, ParamValue[] params) {
		this.invoker.invoke(new WorkloadInvoker(serviceManager, this, params), new ParamHolder(params));
	}
	
	@JsonIgnore
	public Invoker getInvoker() {
		return invoker;
	}
}
