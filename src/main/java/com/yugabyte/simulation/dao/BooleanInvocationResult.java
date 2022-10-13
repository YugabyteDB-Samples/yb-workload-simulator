package com.yugabyte.simulation.dao;

public class BooleanInvocationResult extends InvocationResult{
	private boolean value;
	public BooleanInvocationResult(Exception exception) {
		super(exception);
	}
	
	public BooleanInvocationResult(boolean result) {
		super(Boolean.toString(result));
		this.value = result;
	}
	
	public boolean getValue() {
		return this.value;
	}
}
