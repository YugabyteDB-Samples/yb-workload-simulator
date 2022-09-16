package com.yugabyte.simulation.dao;

public class WorkloadParamDesc {
	private final String name;
	private final ParamType type;
	private final int minValue;
	private final int maxValue;
	private final ParamValue defaultValue;
	private final String[] choices;
	private final String sliderLabel;

	public WorkloadParamDesc(String name, ParamType type, int minValue, int maxValue, ParamValue defaultValue, String sliderLabel) {
		super();
		this.name = name;
		this.type = type;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.defaultValue = defaultValue;
		this.choices = null;
		this.sliderLabel = sliderLabel;
	}
	
	/**
	 * Create a parameter which allows the user to pick from a list of choices. The choices
	 * are all string choices, and the defaultChoiceIndex must be in the range of 0
	 * to (number of choices - 1)
	 * @param name
	 * @param defaultChoiceIndex
	 * @param choices
	 */
	public WorkloadParamDesc(String name, int defaultChoiceIndex, String ... choices) {
		super();
		this.name = name;
		this.type = ParamType.STRING;
		this.choices = choices;
		this.minValue = 0;
		this.maxValue = 0;
		this.sliderLabel = null;
		if (defaultChoiceIndex >= 0 && defaultChoiceIndex < choices.length) {
			defaultValue = new ParamValue(choices[defaultChoiceIndex]);
		}
		else {
			throw new IllegalArgumentException("Choice index of "+ defaultChoiceIndex + " must be >= 0 and < " + choices.length);
		}
 	}
	
	public WorkloadParamDesc(String name, int minValue, int maxValue) {
		this(name, ParamType.NUMBER, minValue, maxValue, null, null);
	}

	public WorkloadParamDesc(String name, ParamType type, ParamValue defaultValue) {
		this(name, type, Integer.MIN_VALUE, Integer.MAX_VALUE, defaultValue, null);
	}
	
	public WorkloadParamDesc(String name, ParamType type) {
		this(name, type, null);
	}

	public WorkloadParamDesc(String name, int defaultValue) {
		this(name, Integer.MIN_VALUE, Integer.MAX_VALUE, defaultValue);
	}
	
	public WorkloadParamDesc(String name, boolean defaultValue) {
		this(name, ParamType.BOOLEAN, Integer.MIN_VALUE, Integer.MAX_VALUE, new ParamValue(defaultValue), null);
	}
	
	public WorkloadParamDesc(String name, String defaultValue) {
		this(name, ParamType.STRING, Integer.MIN_VALUE, Integer.MAX_VALUE, new ParamValue(defaultValue), null);
	}
	
	public WorkloadParamDesc(String name, int minValue, int maxValue, int defaultValue) {
		this(name, ParamType.NUMBER, minValue, maxValue, new ParamValue(defaultValue), null);
	}

	public WorkloadParamDesc(String name, int minValue, int maxValue, int defaultValue, String sliderLabel) {
		// NB: For now the value of the slider label is ignored, it's non-falsy precense is enough to trigger the scroll bar.
		this(name, ParamType.NUMBER, minValue, maxValue, new ParamValue(defaultValue), sliderLabel);
	}

	public String getName() {
		return name;
	}

	public ParamType getType() {
		return type;
	}

	public int getMinValue() {
		return minValue;
	}

	public int getMaxValue() {
		return maxValue;
	}
	
	public ParamValue getDefaultValue() {
		return defaultValue;
	}
	
	public String[] getChoices() {
		return choices;
	}
	
	public String getSliderLabel() {
		return sliderLabel;
	}
}
