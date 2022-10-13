package com.yugabyte.simulation.dao;

public class LoginInformation {
	private String email;
	private String initVector;
	private String validation;

	public LoginInformation() {
	}
	
	public LoginInformation(String email, String initVector, String validation) {
		super();
		this.email = email;
		this.initVector = initVector;
		this.validation = validation;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getInitVector() {
		return initVector;
	}
	public void setInitVector(String initVector) {
		this.initVector = initVector;
	}
	
	public String getValidation() {
		return validation;
	}

	public void setValidation(String validation) {
		this.validation = validation;
	}

	public LoginInformation clone() {
		return new LoginInformation(email, initVector, validation);
	}
}