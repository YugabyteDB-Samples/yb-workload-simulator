package com.yugabyte.simulation.exception;

public class InvalidPasswordException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5917877433605225902L;

	public InvalidPasswordException() {
		super("The supplied password was invalid");
	}
}
