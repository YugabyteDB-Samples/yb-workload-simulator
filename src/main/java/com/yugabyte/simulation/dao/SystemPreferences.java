package com.yugabyte.simulation.dao;

public class SystemPreferences {
	private String workloadName;
	private String loggingDir;
	private boolean doLogging;
	private int graphRefreshMs;
	private int networkRefreshMs;
	
	public String getLoggingDir() {
		return loggingDir;
	}
	public void setLoggingDir(String loggingDir) {
		this.loggingDir = loggingDir;
	}
	public boolean isDoLogging() {
		return doLogging;
	}
	public void setDoLogging(boolean doLogging) {
		this.doLogging = doLogging;
	}
	public String getWorkloadName() {
		return workloadName;
	}
	public void setWorkloadName(String workloadName) {
		this.workloadName = workloadName;
	}
	public int getGraphRefreshMs() {
		return graphRefreshMs;
	}
	public void setGraphRefreshMs(int graphRefreshMs) {
		this.graphRefreshMs = graphRefreshMs;
	}
	public int getNetworkRefreshMs() {
		return networkRefreshMs;
	}
	public void setNetworkRefreshMs(int networkRefreshMs) {
		this.networkRefreshMs = networkRefreshMs;
	}
	
	
}
