package com.heroku.eclipse.core.services;

import java.util.List;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.Key;
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
	public List<App> listApps() throws HerokuServiceException;

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
	 *            the SSH-Key to remove from the account (the description of the key e.g. username@hostname)
	 * @throws HerokuServiceException
	 *             if removing the key fails or {@link #isValid()} is false
	 */
	public void removeSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * lists the added SSH2 keys from the Heroku account
	 * 
	 * @return the registered keys
	 * @throws HerokuServiceException
	 * 				if {@link #isValid()} is false
	 */
	public List<Key> listSSHKeys() throws HerokuServiceException;
	
	
	/**
	 * @return <code>true</code> if the session is valid
	 */
	public boolean isValid();

	/**
	 * @return the API key attached to this session
	 */
	public String getAPIKey();
	
	/**
	 * creates a new App.
	 * 
	 * @return the newly created App
	 * @throws HerokuServiceException
	 * 				if {@link #isValid()} is false
	 */
	public App createApp() throws HerokuServiceException;
	
	/**
	 * creates a new app.
	 * 
	 * @param app an {@link App} object with name and or stack filled.
	 * @return the newly created app
	 * @throws HerokuServiceException
	 *              if {@link #isValid()} is false
	 */
	public App createApp(App app) throws HerokuServiceException;
	
	/**
	 * renames an app.
	 * 
	 * @param currentName the current name of the app
	 * @param newName the new name of the app
	 * @return the new name of the app
	 * @throws HerokuServiceException
	 * 				if {@link #isValid()} is false
	 * 				if newName is already used
	 * 				if currentName does not exist
	 * 				if the request fails
	 */
	public String renameApp(String currentName, String newName) throws HerokuServiceException;
	
	/**
	 * destroys an App identified by name.
	 * 
	 * @param name the app to destroy.
	 * @throws HerokuServiceException
	 * 				if {@link #isValid()} is false
	 * 				if name is invalid
	 * 				if the request fails
	 */
	public void destroyApp(String name) throws HerokuServiceException;
	
	/**
	 * Clones the given template and delivers it as a ready to use Heroku App
	 * @param templateName
	 * @return the newly created App
	 * @throws HerokuServiceException
	 * 				if {@link #isValid()} is false
	 * 				if name is invalid
	 * 				if the request fails
	 */
	public App cloneApp( String templateName ) throws HerokuServiceException;
	
	public void restart(App app) throws HerokuServiceException;

	public void destroyApp(App app) throws HerokuServiceException;
	
	public List<Collaborator> getCollaborators(App app) throws HerokuServiceException;
}
