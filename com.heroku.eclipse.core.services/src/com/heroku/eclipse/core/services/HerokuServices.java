package com.heroku.eclipse.core.services;

import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Interface defining how Heroclipse talks with the com.heroku.api.HerokuAPI API   
 * 
 * @author udo.rader@bestsolution.at
 */
public interface HerokuServices {
	/**
	 * Logs into the Heroku account and if successful, returns the
	 * user's associated API key.
	 * Invokes HerokuAPI.obtainApiKey
	 * @see {@link com.heroku.api.HerokuAPI#obtainApiKey}
	 * @param username
	 * @param password
	 * @return the Heroku API key
	 * @throws HerokuServiceException 
	 */
	public String obtainAPIKey( String username, String password ) throws HerokuServiceException;
	
	/**
	 * Sets the Heroku API key to use for further service calls 
	 * @param apiKey the Heroku API key
	 */
	public void setAPIKey( String apiKey );

	/**
	 * Delivers the Heroku API key stored in the preferences 
	 * @return the Heroku API key
	 */
	public String getAPIKey();

	/**
	 * Delivers the SSH key stored in the global eclipse preferences 
	 * @return the SSH key
	 */
	public String getSSHKey();

	/**
	 * 
	 * @return
	 * @throws HerokuServiceException 
	 */
	public HerokuSession getOrCreateHerokuSession() throws HerokuServiceException;
}
