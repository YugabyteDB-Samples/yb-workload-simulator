package com.yugabyte.simulation.workload;

import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.TimerResult;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadResult;
import com.yugabyte.simulation.services.ExecutionStatus;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.services.Timer;

public class FixedStepsWorkloadType extends WorkloadType {
	private final String[] steps;
	private final Step[] stepList;
	
	public interface ExecuteStep {
		public void run(int stepNum, String stepName);
	};
	
	public static class FixedStepsWorkloadResult extends WorkloadResult {
		private final int currentStepNumber;
		private final WorkloadStep[] steps;
		public FixedStepsWorkloadResult(long fromTime, FixedStepWorkloadInstance instance) {
			super(fromTime, instance);
			this.currentStepNumber = instance.currentStepNumber;
			this.steps = instance.workloadSteps;
		}
		
		public int getCurrentStepNumber() {
			return currentStepNumber;
		}
		
		public WorkloadStep[] getSteps() {
			return steps;
		}
	}
	
	public static class FixedStepsTimerResult extends TimerResult {
		private final int currentStepNumber;
		public FixedStepsTimerResult(TimerResult orig, int currentStepNumber) {
			super(orig);
			this.currentStepNumber = currentStepNumber;
		}
		
		public int getCurrentStepNumber() {
			return currentStepNumber;
		}
	}
	

	public class FixedStepWorkloadInstance extends WorkloadTypeInstance {
		private final WorkloadStep[] workloadSteps;
		private volatile int currentStepNumber = 0;
		private Thread workerThread;

		
		public FixedStepWorkloadInstance(ServiceManager serviceManager, WorkloadDesc workload, ParamValue[] params) {
			super(serviceManager, workload, params);
			if (steps != null) {
				workloadSteps = new WorkloadStep[steps.length];
				for (int i = 0; i < steps.length; i++) {
					workloadSteps[i] = new WorkloadStep(steps[i]);
				}
			}
			else {
				workloadSteps = new WorkloadStep[stepList.length];
				for (int i = 0; i < stepList.length; i++) {
					workloadSteps[i] = new WorkloadStep(stepList[i].stepName);
				}
			}
		}
		public FixedStepWorkloadInstance(ServiceManager serviceManager) {
			this(serviceManager, null, null);
		}
		
		@Override
		public WorkloadType getType() {
			return FixedStepsWorkloadType.this;
		}
		
		private static final String csvHeader = "Start Time,Min Time Us,Average Time Us,Max Time Us,Num Succeeded,Num Failed,Current Step\n";
		private static final String csvFormat = "%d,%d,%d,%d,%d,%d,%d\n";
		
		@Override
		public String formatToCsv(TimerResult result) {
			return String.format(csvFormat, result.getStartTimeMs(), result.getMinUs(), result.getAvgUs(),
					result.getMaxUs(), result.getNumSucceeded(), result.getNumFailed(), 
					((FixedStepsTimerResult)result).getCurrentStepNumber());
		}
		
		@Override
		public String getCsvHeader() {
			return csvHeader;
		}
		
		public WorkloadStep getCurrentStep() {
			int i;
			for (i = 0; i < workloadSteps.length && workloadSteps[i].isComplete(); i++);
			if (i < workloadSteps.length) {
				return workloadSteps[i];
			}
			else {
				return null;
			}
		}

		@Override
		public boolean isComplete() {
			return workloadSteps[workloadSteps.length-1].isComplete();
		}
		
		public void execute(ExecuteStep runner) {
			this.workerThread = new Thread(() -> {
				while (!isComplete() && !isTerminated()) {
					WorkloadStep currentStep = getCurrentStep();
					Timer timer = this.getTimerService().getTimer().start();
					try {
						runner.run(currentStepNumber, currentStep.getName());
						currentStep.complete(timer.end(ExecutionStatus.SUCCESS, this.getWorkloadOrdinal()));
						currentStepNumber++;
						System.out.printf("Step %d (%s) completed in %fms\n", currentStepNumber, currentStep.getName(), currentStep.getExecutionTimeInNs()/1000000.0);
					}
					catch (Exception e) {
						this.setTerminatedByException(e);
						currentStep.complete(timer.end(ExecutionStatus.ERROR, this.getWorkloadOrdinal()));
						handleException(e);
						throw e;
					}
				}
				this.terminate();
			}, this.getWorkloadId());
			this.workerThread.setDaemon(true);
			this.workerThread.start();
		}
		
		public void execute() {
			this.workerThread = new Thread(() -> {
				while (!isComplete() && !isTerminated()) {
					WorkloadStep currentStep = getCurrentStep();
					Timer timer = this.getTimerService().getTimer().start();
					try {
						stepList[currentStepNumber].step.run(currentStepNumber, currentStep.getName());
						currentStep.complete(timer.end(ExecutionStatus.SUCCESS, this.getWorkloadOrdinal()));
						currentStepNumber++;
						System.out.printf("Step %d (%s) completed in %fms\n", currentStepNumber, currentStep.getName(), currentStep.getExecutionTimeInNs()/1000000.0);
					}
					catch (Exception e) {
						this.setTerminatedByException(e);
						currentStep.complete(timer.end(ExecutionStatus.ERROR, this.getWorkloadOrdinal()));
						throw e;
					}
				}
				this.terminate();
			}, this.getWorkloadId());
			this.workerThread.setDaemon(true);
			this.workerThread.start();
		}
		
		// We need to provide the current step number in the workload result
		@Override
		public WorkloadResult getWorkloadResult(long afterTime) {
			return new FixedStepsWorkloadResult(afterTime, this);
		}
		@Override
		protected TimerResult doAugmentTimingResult(TimerResult result) {
			return new FixedStepsTimerResult(result, currentStepNumber);
		}
	}

	public FixedStepsWorkloadType(Step ... steps) {
		this.steps = null;
		this.stepList = steps;
	}
	
	public FixedStepsWorkloadType(String ... steps) {
		this.steps = steps;
		this.stepList = null;
	}
	
	@Override
	public String getTypeName() {
		return "FIXED_STEP";
	}

	@Override
	public FixedStepWorkloadInstance createInstance(ServiceManager serviceManager) {
		return new FixedStepWorkloadInstance(serviceManager);
	}

	@Override
	public WorkloadTypeInstance createInstance(ServiceManager serviceManager, WorkloadDesc workload,
			ParamValue[] params) {
		return new FixedStepWorkloadInstance(serviceManager, workload, params);
	}
	
}