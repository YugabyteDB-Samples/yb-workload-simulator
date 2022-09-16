package com.yugabyte.simulation.controller;

import com.yugabyte.simulation.dao.*;
import com.yugabyte.simulation.service.WorkloadInvoker;
import com.yugabyte.simulation.service.WorkloadSimulation;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.services.SystemPreferencesService;
import com.yugabyte.simulation.workload.WorkloadManager;
import com.yugabyte.simulation.workload.WorkloadTypeInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkloadSimulationController {
    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    private SystemPreferencesService systemPreferencesService;
    
    @Autowired
    private ApplicationContext appContext;
    
    @Autowired
    private ServiceManager serviceManager;
    
    // Generic interface, to be populated with class loaded dynamically?
    @Autowired
    @Resource(name="${spring.workload:genericWorkload}")
    // we can use @Qualifier as well
    private WorkloadSimulation workloadSimulation;
    
    private static final String WORKLOAD_TYPE = "workloadType";
    private static final String WORKLOAD_PARAMS = "params";
    private static final String LOGGING_DIR_PARAM = "loggingDir";

    private List<WorkloadDesc> workloads = null;

    private class WorkloadRunner implements Runnable {
    	private ParamValue[] paramsToUse;
    	private final List<WorkloadParamDesc> neededParams; 
    	private final WorkloadDesc workload;
    	
    	public WorkloadRunner(String workloadToRun) {
    		List<WorkloadDesc> workloads = workloadSimulation.getWorkloads();
    		WorkloadDesc thisWorkload = null;
    		for (WorkloadDesc desc : workloads) {
    			if (desc.getName().equals(workloadToRun)) {
    				thisWorkload = desc;
    				break;
    			}
    			else if (desc.getWorkloadId().equals(workloadToRun)) {
    				thisWorkload = desc;
    				break;
    			}
    		}
    		
    		if (thisWorkload == null) {
    			System.err.printf("Could not find a workload named '%s' on '%s'\nValid workloads are:\n",
    					workloadToRun, workloadSimulation.getName());
    			for (WorkloadDesc desc : workloads) {
    				System.err.printf("    %s (%s) Params: %s\n", desc.getName(), desc.getWorkloadId(), getParamDesc(desc.getParams()));
    			}
    			exit(-1);
    		}
    		this.workload = thisWorkload;
    		neededParams = thisWorkload.getParams();
			String paramString = System.getProperty(WORKLOAD_PARAMS, "");
			try {
				this.paramsToUse = getParamsFromString(thisWorkload, paramString);
			}
			catch (Exception e) {
				System.err.printf("Workload %s (%s) requires parameters, but parameter string given of '%s' does not match '%s'\n",
						this.workload.getName(),
						this.workload.getWorkloadId(),
						paramString,
						getParamDesc(neededParams));
				exit(-3);
			}
    	}
    	
    	@Override
    	public void run() {
    		System.out.printf("Starting workload '%s' (%s) with params:\n", workload.getName(), workload.getWorkloadId());
    		for (int i = 0; i < paramsToUse.length; i++) {
    			System.out.printf("% 4d: %s = %s\n", i+1, neededParams.get(i).getName(), paramsToUse[i].toString());
    		}

    		if (workload.getInvoker() != null) {
    			WorkloadInvoker invoker = new WorkloadInvoker(serviceManager, workload, paramsToUse);
    			workload.getInvoker().invoke(invoker, new ParamHolder(paramsToUse));
    		}
    		else {
    			workloadSimulation.invokeWorkload(workload.getWorkloadId(), paramsToUse);
    		}
			
	    	// Autoterminate the spring boot process
	    	try {
		    	do {
		    		Thread.sleep(1000);
		    	} while (workloadManager.getActiveWorkloads().size() > 1); // Note aggregation workload will be running
	    	}
	    	catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	finally {
	    		exit(0);
	    	}
    	}
    	
    	private ParamValue[] getParamsFromString(WorkloadDesc thisWorkload, String paramString) {
 
    		ParamValue[] values = new ParamValue[neededParams.size()];
    		String[] params = paramString.split(",");
    		int paramIndex = 0;
    		for (WorkloadParamDesc thisNeededParam : neededParams) {
    			if (params[paramIndex].length() == 0) {
    				// Use the default parameter
    				if (thisNeededParam.getDefaultValue() == null) {
    					System.err.printf("Parameter %d (%s) of workload %s was requested to be the default but has no default value\n",
    							paramIndex+1, thisNeededParam.getName(), thisWorkload.getName());
    					exit(-2);
    				}
    				else {
    					values[paramIndex++] = thisNeededParam.getDefaultValue();
    				}
    			}
    			else {
    				// value specified, use it.
    				switch(thisNeededParam.getType()) {
    				case BOOLEAN:
    					values[paramIndex] = new ParamValue(Boolean.parseBoolean(params[paramIndex]));
    					break;
    				case NUMBER:
    					values[paramIndex] = new ParamValue(Integer.parseInt(params[paramIndex]));
    					break;
    				case STRING:
    					values[paramIndex] = new ParamValue(params[paramIndex]);
    					break;
    				}
    				paramIndex++;
    			}
    		}
    		return values;
    	}
    	
    	private String getParamDesc(List<WorkloadParamDesc> params) {
    		StringBuffer sb = new StringBuffer();
    		for (int i= 0; i < params.size(); i++) {
    			WorkloadParamDesc thisParam = params.get(i);
    			sb.append(thisParam.getName()).append(" : ");
    			
    			switch (thisParam.getType()) {
    			case BOOLEAN: sb.append("boolean"); break;
    			case STRING: sb.append("String"); break;
    			case NUMBER: 
    				sb.append("int");
    				if (thisParam.getMinValue() != Integer.MIN_VALUE) {
    					sb.append(" [").append(thisParam.getMinValue()).append(",");
    					if (thisParam.getMaxValue() != Integer.MAX_VALUE) {
    						sb.append(thisParam.getMaxValue());
    					}
    					sb.append(']');
    				}
    				else if (thisParam.getMaxValue() != Integer.MAX_VALUE) {
    					sb.append(" [,").append(thisParam.getMaxValue()).append("]");
    					
    				}
        			break;
    			}
    			if (thisParam.getDefaultValue() != null) {
    				sb.append(" (default: ").append(thisParam.getDefaultValue().toString()).append(")");
    			}
    			if (i < params.size() - 1) {
    				sb.append(", ");
    			}
    		}
    		return sb.toString();
    	}
    	
    	private void exit(int returnCode) {
    		SpringApplication.exit(appContext, () -> returnCode);
    		System.exit(returnCode);
    	}

    }

    @PostConstruct
	private void setWorkloadName() {
    	this.systemPreferencesService.setName(workloadSimulation.getName());
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void checkForWorkload() {
    	String loggingDir = System.getProperty(LOGGING_DIR_PARAM);
    	if (loggingDir != null) {
    		systemPreferencesService.setLoggingPreferences(true, loggingDir);
    	}

    	// See if there is a workload to run
    	String workloadToRun = System.getProperty(WORKLOAD_TYPE);
    	if (workloadToRun != null) {
    		
    		Thread thread = new Thread(new WorkloadRunner(workloadToRun));
    		thread.setDaemon(false);
    		thread.start();
    	}    	
	}

    @GetMapping("get-workloads")
    public synchronized List<WorkloadDesc> getWorkloads() {
    	if (workloads == null) {
    		this.workloads = workloadSimulation.getWorkloads();
    	}
    	return workloads;
    }

    @PostMapping("/invoke-workload/{workload}") 
    @ResponseBody
    public InvocationResult invokeWorkload(@PathVariable String workload, @RequestBody ParamValue[] params) {
    	List<WorkloadDesc> allWorkloads = getWorkloads();
		for (WorkloadDesc aWorkload : allWorkloads) {
			if (aWorkload.getWorkloadId().equals(workload) && aWorkload.getInvoker() != null) {
				try {
					aWorkload.invoke(serviceManager, params);
					return new InvocationResult("Ok");
				}
				catch (Exception e) {
					return new InvocationResult(e);
				}
			}
		}
    	return workloadSimulation.invokeWorkload(workload, params);
    }
    
    @GetMapping("get-active-workloads")
    public List<WorkloadResult> getActiveWorkloads() {
    	List<WorkloadTypeInstance> activeWorkloads = workloadManager.getActiveWorkloads();
    	List<WorkloadResult> statuses = new ArrayList<WorkloadResult>();
    	for (WorkloadTypeInstance instance : activeWorkloads) {
    		if (instance.getType().canBeTerminated()) {
    			statuses.add(instance.getWorkloadResult(Long.MAX_VALUE));
    		}
    	}
    	return statuses;
    }

    @PostMapping("save-system-preferences")
    @ResponseBody
    public InvocationResult saveSystemPreferences(@RequestBody SystemPreferences preferences) {
    	this.systemPreferencesService.saveSystemPreferences(preferences);
    	return new InvocationResult("Ok");
    }
    
    @GetMapping("get-system-preferences")
    public SystemPreferences getSystemPreferences() {
    	return this.systemPreferencesService.getSystemPreferences();
    }
    

    @GetMapping("terminate-workload/{workloadId}")
    public InvocationResult terminateWorkload(@PathVariable String workloadId) {
    	workloadManager.terminateWorkload(workloadId);
    	return new InvocationResult("Ok");
    }
    
    @GetMapping("/getResults/{afterTime}")
    @ResponseBody
    public Map<String, WorkloadResult> getResults(
    		@PathVariable(name = "afterTime") long afterTime) {
    	
    	return workloadManager.getResults(afterTime);
    }
}

