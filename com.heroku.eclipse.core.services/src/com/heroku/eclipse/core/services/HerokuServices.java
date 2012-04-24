package com.heroku.eclipse.core.services;

import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

public interface HerokuServices {
	// GET https://username:password@api.heroku.com/login
	/**
	 * Logs into the Heroku account and if successful, returns the
	 * user's associated API key.
	 * Invokes GET https://username:password@api.heroku.com/login
	 * @param username
	 * @param password
	 * @return the Heroku API key
	 */
	public String getAPIKey( String username, String password ) throws HerokuServiceException;

	/**
	 * Lists all apps associated with the currently logged in account.
	 * Invokes GET https://username:password@api.heroku.com/vendor/apps
	 * @return the list of registered applications
	 */
	public String[] getAllApps() throws HerokuServiceException;

}
