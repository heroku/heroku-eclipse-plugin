package com.heroku.eclipse.core.services.exceptions;

public class HerokuServiceException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int UNKNOWN_ERROR = 0;
	
	public static final int LOGIN_FAILED_ERROR_CODE = 1;
	
	private final int errorCode;
	
	public HerokuServiceException(Throwable t) {
		this(UNKNOWN_ERROR, t);
	}
	
	public HerokuServiceException(int errorCode, Throwable t) {
		super(t);
		this.errorCode = errorCode;
	}
	
	public HerokuServiceException(String message, Throwable t) {
		this(UNKNOWN_ERROR, message, t);
	}
	
	public HerokuServiceException(int errorCode, String message, Throwable t) {
		super(message, t);
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
