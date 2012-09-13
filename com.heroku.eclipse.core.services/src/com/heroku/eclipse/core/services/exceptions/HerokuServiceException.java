package com.heroku.eclipse.core.services.exceptions;

/**
 * @author udo.rader@bestsolution.at
 */
public class HerokuServiceException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * error code indicating that we have no clue about ... it.
	 */
	public static final int UNKNOWN_ERROR = 0;

	/**
	 * error code indicating that a login attempt failed
	 */
	public static final int LOGIN_FAILED = 1;

	/**
	 * error code indicating a missing Heroku API key
	 */
	public static final int NO_API_KEY = 2;

	/**
	 * error code indicating that the Heroku session is invalid
	 */
	public static final int INVALID_SESSION = 3;

	/**
	 * error code indicating an invalid Heroku API key
	 */
	public static final int INVALID_API_KEY = 4;

	/**
	 * error code indicating that we have troubles accessing Eclipse's secure
	 * store
	 */
	public static final int SECURE_STORE_ERROR = 5;

	/**
	 * error code indicating that this SSH key is invalid
	 */
	public static final int INVALID_SSH_KEY = 6;

	/**
	 * error code indicating that this SSH key already exists
	 */
	public static final int SSH_KEY_ALREADY_EXISTS = 7;

	/**
	 * error code indicating that a network request failed
	 */
	public static final int REQUEST_FAILED = 8;

	/**
	 * error code indicating that a network resource was not found
	 */
	public static final int NOT_FOUND = 9;

	/**
	 * error code indicating that we were not allowed to access a network
	 * resource or perform a specific operation
	 */
	public static final int NOT_ALLOWED = 10;

	/**
	 * error code indicating that we were not allowed to perform an action on a
	 * network resource, for example when we assign an application name that already
	 * exists.
	 */
	public static final int NOT_ACCEPTABLE = 11;

	/**
	 * error code indicating that the plugin's preferences are invalid, e.g. not
	 * set up at all
	 */
	public static final int INVALID_PREFERENCES = 12;

	/**
	 * the location intended to contain files from git is invalid, not accessible or already exists
	 */
	public static final int INVALID_LOCAL_GIT_LOCATION = 13;

	/**
	 * indicates that there is insufficient data to complete the intended action
	 */
	public static final int INSUFFICIENT_DATA = 14;

	/**
	 * indicates that the user has cancelled an operation
	 */
	public static final int OPERATION_CANCELLED = 15;
	
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
		this(errorCode, message, null);
	}

	public HerokuServiceException(int errorCode, String message, Throwable t) {
		super(message, t);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
