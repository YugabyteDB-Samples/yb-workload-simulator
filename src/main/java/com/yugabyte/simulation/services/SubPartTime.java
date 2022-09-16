package com.yugabyte.simulation.services;

public class SubPartTime {
	private long timeDelta;
	private String name;
	
	public SubPartTime(String name, long time) {
		this.name = name;
		this.timeDelta = time;
	}
	
	public String getName() {
		return name;
	}
	public long getTimeDelta() {
		return timeDelta;
	}
}
