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
	 * @return the list of registered applications
	 * @throws HerokuServiceException 
	 */
	public List<App> getAllApps() throws HerokuServiceException;
	
	/**
	 * Adds the given SSH2 key to the Heroku account
	 * @param sshKey 
	 * @throws HerokuServiceException
	 */
	public void addSSHKey( String sshKey ) throws HerokuServiceException;

	/**
	 * Removes the given SSH2 key from the Heroku account
	 * @param sshKey 
	 * @throws HerokuServiceException
	 */
	public void removeSSHKey( String sshKey ) throws HerokuServiceException;
	
	public boolean isValid();
}
