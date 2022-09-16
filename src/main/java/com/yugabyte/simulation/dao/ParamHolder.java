package com.yugabyte.simulation.dao;

public class ParamHolder {
	private final ParamValue[] params;
	public ParamHolder(ParamValue[] params) {
		this.params = params;
	}
	
	public ParamValue get(int index) {
		return params[index];
	}
	
	public int asInt(int index) {
		return get(index).getIntValue();
	}
	
	public boolean asBool(int index) {
		return get(index).getBoolValue();
	}

	public String asString(int index) {
		return get(index).getStringValue();
	}
}

