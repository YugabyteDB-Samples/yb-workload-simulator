package com.yugabyte.simulation.dao;

public class TimerResult {
	private final long numSucceeded;
	private final long numFailed;
	private final long minUs;
	private final long maxUs;
	private final long avgUs;
	private final long startTimeMs;
	
	public TimerResult(long[] succeededTimes, int succeededCounts,
				long[] failedTimes, int failedCounts, long startTimeMs) {
		
		this.numFailed = failedCounts;
		this.numSucceeded = succeededCounts;
		this.startTimeMs = startTimeMs;
		
		long minUs = Long.MAX_VALUE;
		long maxUs = Long.MIN_VALUE;
		long totalTime = 0;
		for (int j = 0; j < 2; j++) {
			long[] data = (j == 0 ? succeededTimes : failedTimes);
			int count = (j == 0 ? succeededCounts : failedCounts);
			for (int i = 0; i < count; i++) {
				long time = data[i];
				if (time < minUs) {
					minUs = time;
				}
				if (time > maxUs) {
					maxUs = time;
				}
				totalTime += time;
			}
		}
		if (minUs < Long.MAX_VALUE) {
			this.minUs = minUs;
			this.maxUs = maxUs;
			this.avgUs = totalTime / (failedCounts + succeededCounts);

			// If StdDev is required see https://www.programiz.com/java-programming/examples/standard-deviation
		}
		else {
			this.minUs = 0;
			this.maxUs = 0;
			this.avgUs = 0;
		}
	}
	
	protected TimerResult(TimerResult original) {
		this.numFailed = original.numFailed;
		this.numSucceeded = original.numSucceeded;
		this.avgUs = original.avgUs;
		this.maxUs = original.maxUs;
		this.minUs = original.minUs;
		this.startTimeMs = original.startTimeMs;
	}

	public long getNumSucceeded() {
		return numSucceeded;
	}

	public long getNumFailed() {
		return numFailed;
	}

	public long getMinUs() {
		return minUs;
	}

	public long getMaxUs() {
		return maxUs;
	}

	public long getAvgUs() {
		return avgUs;
	}

	public long getStartTimeMs() {
		return startTimeMs;
	}
	
	@Override
	public String toString() {
		return String.format("Ops/s: %,d (%,d, %,d), min: %,dus, avg: %,dus, max: %,dus\n",
				(numSucceeded + numFailed),
				numSucceeded,
				numFailed,
				minUs,
				avgUs,
				maxUs);
	}
}
