package com.heroku.eclipse.core.services;

import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Interface defining how Heroclipse talks with the com.heroku.api.HerokuAPI API
 * 
 * @author udo.rader@bestsolution.at
 */
public interface HerokuServices {
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
	 * Sets the Heroku API key to use for further service calls.
	 * 
	 * <p>
	 * The API key is validated before stored
	 * </p>
	 * 
	 * @param apiKey
	 *            the Heroku API key, might be <code>null</code> null to reset it
	 * @throws HerokuServiceException
	 *             if storage of the key fails or api key is invalid
	 */
	public void setAPIKey(String apiKey) throws HerokuServiceException;

	/**
	 * Delivers the Heroku API key stored in the preferences
	 * 
	 * @return the Heroku API key
	 */
	public String getAPIKey();

	/**
	 * Delivers the SSH key stored in the global eclipse preferences
	 * 
	 * @return the SSH key
	 */
	public String getSSHKey();

	/**
	 * Store the SSH key stored in the global eclipse preferences
	 * 
	 * @param sshKey
	 *            the SSH key, might be <code>null</code> null to reset it
	 * @throws HerokuServiceException
	 *             if storage of the SSH key fails
	 */
	public void setSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * Returns and existing heroku session of the currently set API key or
	 * creates a new one in case there's none created yet
	 * 
	 * @return a valid heroku session for the currently configured API key
	 * @throws HerokuServiceException
	 *             if the session could not be created (e.g. becasuse of a
	 *             missing API key)
	 */
	public HerokuSession getOrCreateHerokuSession()
			throws HerokuServiceException;

	/**
	 * Validates the API key
	 * 
	 * @param apiKey
	 *            the API key to validate
	 * @throws HerokuServiceException
	 *             if the key is invalid
	 */
	public void validateAPIKey(String apiKey) throws HerokuServiceException;

	/**
	 * Validates the SSH key
	 * 
	 * @param sshKey
	 *            the SSH key to validate
	 * @throws HerokuServiceException
	 *             if the key is invalid
	 */
	public void validateSSHKey(String sshKey) throws HerokuServiceException;
}
