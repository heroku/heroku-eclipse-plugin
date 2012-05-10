package com.heroku.eclipse.core.services;

import com.heroku.api.Key;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Interface defining how Heroclipse talks with the com.heroku.api.HerokuAPI API
 * 
 * @author udo.rader@bestsolution.at
 */
public interface HerokuServices {

	/**
	 * Root topic of all heroku events
	 */
	public static final String ROOT_TOPIC = "com/heroku/eclipse/";

	/**
	 * Root topic of all heroku core events
	 */
	public static final String ROOT_CORE_TOPIC = ROOT_TOPIC + "core/";

	/**
	 * Event topic fired if a session is invalidated
	 * 
	 * @see #KEY_SESSION_INSTANCE
	 */
	public static final String TOPIC_SESSION_INVALID = ROOT_CORE_TOPIC
			+ "session/invalid";
	/**
	 * Event topic fired if a session a new session is created
	 * 
	 * @see #KEY_SESSION_INSTANCE
	 */
	public static final String TOPIC_SESSION_CREATED = ROOT_CORE_TOPIC
			+ "session/created";

	/**
	 * Event key holding the session modified
	 * 
	 * @see #TOPIC_SESSION_INVALID
	 * @see #TOPIC_SESSION_CREATED
	 */
	public static final String KEY_SESSION_INSTANCE = "session";

	/**
	 * Logs into the Heroku account and if successful, returns the user's
	 * associated API key. Invokes HerokuAPI.obtainApiKey
	 * 
	 * @see {@link com.heroku.api.HerokuAPI#obtainApiKey}
	 * @param username
	 * @param password
	 * @return the Heroku API key
	 * @throws HerokuServiceException
	 */
	public String obtainAPIKey(String username, String password)
			throws HerokuServiceException;

	/**
	 * Sets the Heroku API key to use for further service calls and stores it in
	 * the global eclipse preferences. If there's an active session, it is
	 * invalidated in case the key changed.
	 * 
	 * <p>
	 * The API key is validated before stored
	 * </p>
	 * 
	 * @param apiKey
	 *            the Heroku API key, might be <code>null</code> to reset it
	 * @throws HerokuServiceException
	 *             if storage of the key fails or api key is invalid
	 * @see HerokuServices#TOPIC_SESSION_INVALID
	 */
	public void setAPIKey(String apiKey) throws HerokuServiceException;

	/**
	 * Delivers the Heroku API key stored in the preferences
	 * 
	 * @throws HerokuServiceException
	 *             if we have problems accessing the secure storage 
	 * @return the Heroku API key
	 */
	public String getAPIKey() throws HerokuServiceException;

	/**
	 * Delivers the SSH key stored in the global eclipse preferences
	 * 
	 * @return the SSH key
	 */
	public String getSSHKey();

	/**
	 * Store the SSH key in the global eclipse preferences
	 * 
	 * @param sshKey
	 *            the SSH key, might be <code>null</code> to reset it
	 * @throws HerokuServiceException
	 *             if storage of the SSH key fails
	 */
	public void setSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * Returns an existing heroku session of the currently set API key or
	 * creates a new one in case there's none created yet
	 * 
	 * @return a valid heroku session for the currently configured API key
	 * @throws HerokuServiceException
	 *             if the session could not be created (e.g. because of a
	 *             missing API key)
	 * @see HerokuServices#TOPIC_SESSION_CREATED
	 */
	public HerokuSession getOrCreateHerokuSession()
			throws HerokuServiceException;

	/**
	 * Validates the API key
	 * 
	 * @param apiKey
	 *            the API key to validate. Set <code>null</code> / empty string
	 *            to remove it
	 * @throws HerokuServiceException
	 *             if the key is invalid
	 */
	public void validateAPIKey(String apiKey) throws HerokuServiceException;

	/**
	 * Validates if the fiven SSH public key well formated
	 * 
	 * @param sshKey
	 *            the SSH key to validate
	 * @throws HerokuServiceException
	 *             if the key is invalid
	 * @return a string array consisting of the key parts
	 */
	public String[] validateSSHKey(String sshKey) throws HerokuServiceException;
}
