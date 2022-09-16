package com.yugabyte.simulation.workload;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.TimerResult;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadResult;
import com.yugabyte.simulation.services.LoggingFileManager;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.services.TimerService;

public abstract class WorkloadTypeInstance {
	private volatile WorkloadStatusType status;
	private Exception terminatingException = null;
	
	private final String workloadId;
	private String description;
	private final long startTime;
	private long endTime = -1;

	public abstract WorkloadType getType();
	public abstract boolean isComplete();
	private final int workloadOrdinal;
	private final ServiceManager serviceManager;
	
	private final List<TimerResult> timingResults;
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadTypeInstance.class);

	public WorkloadTypeInstance(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
		this.startTime = System.currentTimeMillis();
		this.workloadId = createWorkloadId(); 
		this.status = WorkloadStatusType.SUBMITTED;
		this.doInitialize();
		this.status = WorkloadStatusType.EXECUTING;
		this.timingResults = new ArrayList<TimerResult>();
		this.workloadOrdinal = getTimerService().startTimingWorkload(this);
		this.serviceManager.getWorkloadManager().registerWorkloadInstance(this);
	}
	
	public WorkloadTypeInstance(ServiceManager serviceManager, WorkloadDesc workload, ParamValue[] params) {
		this(serviceManager);
		if (workload != null) {
			this.setDescriptionFromParams(workload, params);
		}
	}

	protected TimerResult doAugmentTimingResult(TimerResult result) {
		return result;
	}
	
	protected String createWorkloadId() {
		return getType().getTypeName() + "_" + this.startTime;
	}
	protected void doTerminate() {}
	
	protected void doInitialize() {}

	public final void terminate() {
		this.status = WorkloadStatusType.TERMINATING;
		this.doTerminate();
		this.status = WorkloadStatusType.TERMINATED;
		this.endTime = System.currentTimeMillis();
	}
	
	
	public boolean isTerminated() {
		return WorkloadStatusType.TERMINATED.equals(status);
	}
	
	public void setTerminatedByException(Exception e) {
		this.terminatingException = e;
		this.terminate();
	}
	
	public Exception getTerminatingException() {
		return terminatingException;
	}
	
	public String getWorkloadId() {
		return workloadId;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public long getEndTime() {
		return endTime;
	}
	
	public WorkloadStatusType getStatus() {
		return status;
	}
	
	public List<TimerResult> getTimingResults() {
		return timingResults;
	}
	
	public abstract String formatToCsv(TimerResult result);
	public abstract String getCsvHeader();
	
	public TimerResult submitTimingResult(TimerResult result, int maxLength) {
		synchronized (timingResults) {
			TimerResult newResult = doAugmentTimingResult(result);
			timingResults.add(newResult);
			if (timingResults.size() > maxLength) {
				timingResults.remove(0);
			}
			return newResult;
		}
	}
	protected TimerService getTimerService() {
		return serviceManager.getTimerService();
	}
	
	protected LoggingFileManager getLoggingManager() {
		return serviceManager.getLoggingFileManager();
	}
	protected int getWorkloadOrdinal() {
		return this.workloadOrdinal;
	}
	
	private Exception lastException = null;
	protected void handleException(Exception e) {
		if (LOGGER.isErrorEnabled()) {
			if (lastException == null || lastException.getClass() != e.getClass() || 
					(e.getStackTrace().length > 0 && lastException.getStackTrace().length > 0 && !e.getStackTrace()[0].toString().equals( lastException.getStackTrace()[0].toString()))) {
					
				System.err.println("Exception thrown from workload " + this.getWorkloadId() + " of type "+ this.getType().getTypeName());
				e.printStackTrace();
				lastException = e;
			}
			else {
				System.err.print("\"");
			}
		}
	}
	
	public List<TimerResult> getResults(long fromTime) {
		synchronized (timingResults) {
			if (fromTime <= 0) {
				return this.timingResults;
			}
			else {
				// Return a sub-array containing the correct elements which
				// are greater than fromTime. Do a binary search for this.
				// Binary search for the right element
				int length = timingResults.size();
				int start = 0;
				int end = length-1;
				int index = -1;
				while (start <= end) {
					int mid = (start + end)/2;
					// Move to the right side if the target is greater
					if (timingResults.get(mid).getStartTimeMs() <= fromTime) {
						start = mid + 1;
					}
					else {
						// Move left side
						index = mid;
						end = mid - 1;
					}
				}
				if (index >= 0) {
					return timingResults.subList(index, length);
				}
				else {
					return new ArrayList<TimerResult>();
				}
			}
		}
	}
	
	public WorkloadResult getWorkloadResult(long afterTime) {
		return new WorkloadResult(afterTime, this);
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescriptionFromParams(WorkloadDesc workload, ParamValue []params) {
		StringBuffer sb = new StringBuffer();
		sb.append(workload.getName()).append(" (");
		for (int i = 0; i < workload.getParams().size(); i++) {
			sb.append(workload.getParams().get(i).getName()).append(":").append(params[i].toString());
			sb.append(", ");
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		sb.append(" started at:").append(sdf.format(new Date(this.startTime)));
		sb.append(")");
		this.setDescription(sb.toString());
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
