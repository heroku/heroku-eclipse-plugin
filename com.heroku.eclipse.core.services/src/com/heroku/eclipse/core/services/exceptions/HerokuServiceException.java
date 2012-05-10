package com.heroku.eclipse.core.services.exceptions;

public class HerokuServiceException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int UNKNOWN_ERROR = 0;
	public static final int LOGIN_FAILED_ERROR_CODE = 1;
	public static final int NO_API_KEY = 2;
	public static final int INVALID_STATE = 3;
	public static final int INVALID_API_KEY = 4;
	public static final int REQUEST_FAILED = 5;
	public static final int NOT_FOUND = 6;
	public static final int NOT_ALLOWED = 7;
	public static final int NOT_ACCEPTABLE = 8;
	
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
	
	public HerokuServiceException(int errorCode, String message) {
		this(UNKNOWN_ERROR, message, null);
	}
	
	public HerokuServiceException(int errorCode, String message, Throwable t) {
		super(message, t);
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
