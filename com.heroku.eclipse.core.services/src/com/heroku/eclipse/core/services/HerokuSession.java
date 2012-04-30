package com.heroku.eclipse.core.services;

import java.util.List;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Interface defining how various services of the Heroku cloud can be accessed
 * 
 * @author udo.rader@bestsolution.at
 */
public interface HerokuSession {
	/**
	 * Lists all apps associated with the currently logged in account.
	 * 
	 * @return the list of registered applications
	 * @throws HerokuServiceException
	 *             if fetch applications from the backend fails or
	 *             {@link #isValid()} is false
	 */
	public List<App> getAllApps() throws HerokuServiceException;

	/**
	 * Adds the given SSH2 key to the Heroku account
	 * 
	 * @param sshKey
	 *            the SSH-Key to add to the account
	 * @throws HerokuServiceException
	 *             if adding the key fails or {@link #isValid()} is false
	 */
	public void addSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * Removes the given SSH2 key from the Heroku account
	 * 
	 * @param sshKey
	 *            the SSH-Key to remove from the account
	 * @throws HerokuServiceException
	 *             if removing the key fails or {@link #isValid()} is false
	 */
	public void removeSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * @return <code>true</code> if the session is valid
	 */
	public boolean isValid();

	/**
	 * @return the API key attached to this session
	 */
	public String getAPIKey();
}
