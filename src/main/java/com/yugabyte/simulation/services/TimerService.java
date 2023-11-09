package com.yugabyte.simulation.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yugabyte.simulation.dao.TimerResult;
import com.yugabyte.simulation.workload.WorkloadTypeInstance;

@Service
public class TimerService {
	
	@Autowired
	private LoggingFileManager loggingManager;
	
	private class TimerImpl implements Timer {
		private final List<SubPartTime> subPartsTimes = new ArrayList<>();
		
		public TimerImpl() {
		}
		private long startTime;
		
		@Override
		public Timer start() {
			this.startTime = System.nanoTime();
			return this;
		}

		@Override
		public Timer timeSubPortion(String description) {
			this.subPartsTimes.add(new SubPartTime(description,
					System.nanoTime() - startTime));
			return this;
		}
		
		@Override
		public long end(ExecutionStatus status, int workloadOrdinal) {
			long time = System.nanoTime() - startTime;
			TimerService.this.submitResult(time/1000, workloadOrdinal, status);
			return time;
		}
	}

	private final int AGGREGATE_ORDINAL = 0;
	
//	private final Map<String, List<TimerResult>> timingResults;
	private static final int MAX_RESULTS_PER_SECOND = 250000;
	private static final int MAX_RESULTS_SECONDS = 86400;
	private class TimingInstancePerSecond {
		long[] currentSuccesses;
		long[] currentFailures;
		int currentSuccessesCount;
		int currentFailuresCount;
		
		public TimingInstancePerSecond() {
			currentSuccesses = new long[MAX_RESULTS_PER_SECOND];
			currentFailures = new long[MAX_RESULTS_PER_SECOND];
			currentSuccessesCount = 0;
			currentFailuresCount = 0;
		}
	}
	
	private class ResultsAccumulator {
		boolean use1stResult = true;
		final Map<String, Integer> resultsOrdinals;
		final Map<Integer, String> resultsReverseOrdinals;
		final Map<String, WorkloadTypeInstance> workloadMap;
		final List<TimingInstancePerSecond>[] timings;

		private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");



//		long[][] currentSuccesses;
//		long[][] currentFailures;
//		int[] successCount;
//		int[] failureCount;
		
		@SuppressWarnings("unchecked")
		public ResultsAccumulator() {
			timings = new ArrayList[2];
			timings[0] = new ArrayList<TimingInstancePerSecond>();
			timings[1] = new ArrayList<TimingInstancePerSecond>();
			resultsOrdinals = new ConcurrentHashMap<>();
			resultsReverseOrdinals = new ConcurrentHashMap<>();
			workloadMap = new ConcurrentHashMap<String, WorkloadTypeInstance>();

//			getResultIndexForName(AGGREGATE_METRIC_NAME);
			
//			currentSuccesses = new long[2][];
//			currentSuccesses[0] = new long[MAX_RESULTS_PER_SECOND];
//			currentSuccesses[1] = new long[MAX_RESULTS_PER_SECOND];
//			currentFailures = new long[2][];
//			currentFailures[0] = new long[MAX_RESULTS_PER_SECOND];
//			currentFailures[1] = new long[MAX_RESULTS_PER_SECOND];
//			successCount = new int[] {0, 0};
//			failureCount = new int[] {0, 0};
		}

		public int getNextAvailableResultIndex() {
			int i;
			for (i = 0; resultsReverseOrdinals.containsKey(i); i++);
			return i;
		}
		
		public int getResultIndexForName(String name) {
			Integer value = resultsOrdinals.get(name);
			if (value != null) {
				return value;
			}
			else {
				int result = getNextAvailableResultIndex();
				resultsOrdinals.put(name, result);
				resultsReverseOrdinals.put(result, name);
				if (result >= timings[0].size()) {
					timings[0].add(new TimingInstancePerSecond());
					timings[1].add(new TimingInstancePerSecond());
				}
				else {
					timings[0].set(result, new TimingInstancePerSecond());
					timings[1].set(result, new TimingInstancePerSecond());
				}
				return result;
			}
		}
		
		int getCurrentIndex() {
			return use1stResult ? 0 : 1;
		}
		
		void swapIndex() {
			this.use1stResult = !use1stResult;
//			successCount[newIndex] = 0;
//			failureCount[newIndex] = 0;
		}
		
		void submitResult(long timeInUs, int workloadOrdinal, ExecutionStatus status) {
			int index = this.getCurrentIndex();
			int count;
			TimingInstancePerSecond timings = this.timings[index].get(workloadOrdinal);

			synchronized(timings) {
				switch (status) {
				case SUCCESS:
					count = timings.currentSuccessesCount; 
					if (count < MAX_RESULTS_PER_SECOND) {
						timings.currentSuccesses[count] = timeInUs;
						timings.currentSuccessesCount++;
					}
					break;
				case ERROR:
					count = timings.currentFailuresCount; 
					if (count < MAX_RESULTS_PER_SECOND) {
						timings.currentFailures[count] = timeInUs;
						timings.currentFailuresCount++;
					}
					break;
				}
			}
			if (workloadOrdinal != AGGREGATE_ORDINAL) {
				// We need to redo this with the aggregate results
				submitResult(timeInUs, 0, status);
			}
 		}
		
		void submitResult(long timeInUs, String workloadId, ExecutionStatus status) {
			Integer index = this.resultsOrdinals.get(workloadId);
			this.submitResult(timeInUs, index == null? 0 : index, status);
		}

		public synchronized int addTimingWokload(WorkloadTypeInstance workload) {
			int index = getResultIndexForName(workload.getWorkloadId());
			workloadMap.put(workload.getWorkloadId(), workload);
			loggingManager.createFile(workload.getWorkloadId(), workload.getCsvHeader());
			return index;
		}

		public synchronized void removeTimingWorkload(WorkloadTypeInstance workload) {
			workloadMap.remove(workload.getWorkloadId());
			loggingManager.closeFile(workload.getWorkloadId());
			int ordinal = resultsOrdinals.remove(workload.getWorkloadId());
			resultsReverseOrdinals.remove(ordinal);
		}

		public synchronized void accumulateIntervalResults(long startTime, long sampleStartTime) {
			// Need to swap the results over.
			long now = System.currentTimeMillis();
			int indexToAccumulate = getCurrentIndex();
			swapIndex();
			
			for (String workloadId : this.resultsOrdinals.keySet()) {
				int index = this.resultsOrdinals.get(workloadId);
				TimingInstancePerSecond timingsToAnalyze = timings[indexToAccumulate].get(index);
				TimerResult result = new TimerResult(
						timingsToAnalyze.currentSuccesses,
						timingsToAnalyze.currentSuccessesCount,
						timingsToAnalyze.currentFailures,
						timingsToAnalyze.currentFailuresCount,
						sampleStartTime
				);
				
				timingsToAnalyze.currentFailuresCount = 0;
				timingsToAnalyze.currentSuccessesCount = 0;

				Date currentDate = new Date(now);
				String currentTimeStr = dateFormat.format(currentDate);
				
				if (result.getNumFailed() + result.getNumSucceeded() > 0) {
					System.out.printf("[%s] %,dms: %s: %s",
							currentTimeStr,
							now - startTime,
							workloadId,
							result.toString());
				}
				
				WorkloadTypeInstance workload = workloadMap.get(workloadId);
				if (workload != null) {
					TimerResult newResult = workload.submitTimingResult(result, MAX_RESULTS_SECONDS);
					loggingManager.writeLine(workload.getWorkloadId(), workload.formatToCsv(newResult));
				}
				if (workload.isTerminated()) {
					removeTimingWorkload(workload);
				}

			}
		
//			for (TimerType thisType : accumulators.keySet()) {
//				ResultsAccumulator thisAccumulator = accumulators.get(thisType);
//				int index = thisAccumulator.getCurrentIndex();
//				synchronized (thisAccumulator) {
//					thisAccumulator.swapIndexAndZero();
//				}
//				TimerResult result = new TimerResult(
//						thisAccumulator.currentSuccesses[index],
//						thisAccumulator.successCount[index],
//						thisAccumulator.currentFailures[index], 
//						thisAccumulator.failureCount[index],
//						sampleStartTIme);
//				
//				WorkloadDesc workload = activeWorkload;
//				String name = thisType.toString();
//				if (workload != null && workload.getWorkloadName(thisType) != null) {
//					name = workload.getWorkloadName(thisType);
//					
//					System.out.printf("%,dms: %s: %s", 
//							now - startTime,
//							name,
//							result.toString());
//
//				}
//				
//				
//				List<TimerResult> results = timingResults.get(thisType);
//				synchronized(results) {
//					results.add(result);
//					if (results.size() > MAX_RESULTS_SECONDS) {
//						results.remove(0);
//					}
//				}
//			}

		}

	}
	
//	private Map<TimerType, ResultsAccumulator> accumulators;
	private final ResultsAccumulator accumulator;
//	private Map<Long, Long> threadStartTimes = new ConcurrentHashMap<Long, Long>();
//	private WorkloadDesc activeWorkload = null;
	
	private class ResultsCollator implements Runnable {
		private long startTime;
		@Override
		public void run() {
			this.startTime = System.currentTimeMillis();
			while (true) {
				long sampleStartTime = System.currentTimeMillis();
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException ie) {
					break;
				}
				accumulator.accumulateIntervalResults(startTime, sampleStartTime);
			}
		}
	}
	
	public TimerService() {
//		this.timingResults = new ConcurrentHashMap<TimerType, List<TimerResult>>();
//		this.timingResults.put(TimerType.WORKLOAD2, new ArrayList<TimerResult>());
//		this.timingResults.put(TimerType.WORKLOAD1, new ArrayList<TimerResult>());
//
//		this.accumulators = new ConcurrentHashMap<TimerType, TimerService.ResultsAccumulator>();
//		this.accumulators.put(TimerType.WORKLOAD2, new ResultsAccumulator());
//		this.accumulators.put(TimerType.WORKLOAD1, new ResultsAccumulator());

		this.accumulator = new ResultsAccumulator();
		
		Thread collator = new Thread(new ResultsCollator());
		collator.setDaemon(true);
		collator.setName("Results collator");
		collator.setPriority(Thread.MAX_PRIORITY);
		collator.start();
	}

//	public synchronized TimerService setCurrentWorkload(WorkloadDesc workload) {
//		this.activeWorkload = workload;
//		return this;
//	}
//	
//	public synchronized TimerService removeCurrentWorkload(WorkloadDesc workload) {
//		if (this.activeWorkload == workload) {
//			this.activeWorkload = null;
//		}
//		return this;
//	}
	public Timer getTimer() {
		return new TimerImpl();
	}
	
//	public Map<TimerType, List<TimerResult>> getResults(long fromTime) {
//		TimerType thisType = TimerType.WORKLOAD2;
//		List<TimerResult> results = timingResults.get(thisType);
//		synchronized (results) {
//			if (fromTime <= 0) {
//				return this.timingResults;
//			}
//			else {
//				// Return a sub-array containing the correct elements which
//				// are greater than fromTime. Do a binary search for this.
//				// Binary search for the right element
//				int length = results.size();
//				int start = 0;
//				int end = length-1;
//				int index = -1;
//				while (start <= end) {
//					int mid = (start + end)/2;
//					// Move to the right side if the target is greater
//					if (results.get(mid).getStartTimeMs() <= fromTime) {
//						start = mid + 1;
//					}
//					else {
//						// Move left side
//						index = mid;
//						end = mid - 1;
//					}
//				}
//				// all types should have the same indexes so assume this.
//				Map<TimerType, List<TimerResult>> timings = 
//						new HashMap<TimerType, List<TimerResult>>();
//				
//				if (index > -1) {
//					for (TimerType aType : accumulators.keySet()) {
//						timings.put(aType, new ArrayList<TimerResult>(timingResults.get(aType).subList(index, length)));
//					}
//				}
//				return timings;
//			}
//		}
//	}

	public void submitResult(long timeInUs, int workloadOrdinal, ExecutionStatus status) {
		accumulator.submitResult(timeInUs, workloadOrdinal, status);
	}

	public void submitResult(long timeInUs, String workloadId, ExecutionStatus status) {
		accumulator.submitResult(timeInUs, workloadId, status);
	}
	
	public int startTimingWorkload(WorkloadTypeInstance workload) {
		return accumulator.addTimingWokload(workload);
	}
	
	public void stopTimingWorkload(WorkloadTypeInstance workload) {
		accumulator.removeTimingWorkload(workload);
	}
}
