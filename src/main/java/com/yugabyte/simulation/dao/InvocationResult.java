package com.yugabyte.simulation.dao;

public class InvocationResult {
	private String data;
	private int result;
	
	public InvocationResult(String data) {
		super();
		this.data = data;
		this.result = 0;
	}

	public InvocationResult(Exception exception) {
		this.data = exception.getMessage();
		this.result = -1;
	}

	public String getData() {
		return data;
	}
	
	public int getResult() {
		return result;
	}
	
}
