package com.heroku.eclipse.core.services;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.Key;
import com.heroku.api.Proc;
import com.heroku.api.User;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.HerokuDyno;
import com.heroku.eclipse.core.services.model.HerokuProc;

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
	 *            the SSH-Key to remove from the account (the description of the
	 *            key e.g. username@hostname)
	 * @throws HerokuServiceException
	 *             if removing the key fails or {@link #isValid()} is false
	 */
	public void removeSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * lists the added SSH2 keys from the Heroku account
	 * 
	 * @return the registered keys
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false
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
	 * creates a new app.
	 * 
	 * @param app
	 *            an {@link App} object with name and or stack filled.
	 * @return the newly created app
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false
	 */
	public App createApp(App app) throws HerokuServiceException;

	/**
	 * renames an app.
	 * 
	 * @param currentName
	 *            the current name of the app
	 * @param newName
	 *            the new name of the app
	 * @return the new name of the app
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false if newName is already used if
	 *             currentName does not exist if the request fails
	 */
	public String renameApp(String currentName, String newName) throws HerokuServiceException;

	/**
	 * destroys an App identified by name.
	 * 
	 * @param name
	 *            the app to destroy.
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false if name is invalid if the
	 *             request fails
	 */
	public void destroyApp(String name) throws HerokuServiceException;

	/**
	 * Creates a new app based on the given template name. Only works on the
	 * cedar stack. If to App object is passed it, a new, arbitrary App is
	 * created.
	 * 
	 * @param app
	 *            an {@link App} object with name filled. Typically created
	 *            using new App().named(...). May be null. If null, an
	 *            arbitrary, new app is created.
	 * @param templateName
	 * @return the newly created app
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false
	 */
	public App createAppFromTemplate(App app, String templateName) throws HerokuServiceException;

	/**
	 * Clones the given template and delivers it as a ready to use Heroku App
	 * 
	 * @param templateName
	 * @return the newly created App
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false if name is invalid if the
	 *             request fails
	 */
	public App cloneTemplate(String templateName) throws HerokuServiceException;

	/**
	 * Retrieves the named, existing app from the user's Heroku account
	 * 
	 * @param appName
	 * @return the already existing App
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false if name is invalid if the
	 *             request fails
	 */
	public App getApp(String appName) throws HerokuServiceException;

	/**
	 * Delivers all known user information for the currently active session.
	 * 
	 * @return a User object
	 * @throws HerokuServiceException
	 *             if {@link #isValid()} is false if the request fails
	 */
	public User getUserInfo() throws HerokuServiceException;

	/**
	 * Delivers the log stream for the given App
	 * 
	 * The stream remains open as long as it is not closed, so a "tail -f" style
	 * log viewer is possible
	 * 
	 * @param appName
	 * @return the log InputStream
	 * @throws HerokuServiceException
	 */
	public InputStream getApplicationLogStream(String appName) throws HerokuServiceException;

	/**
	 * Delivers the log stream for the given process of the given App.
	 * 
	 * The stream remains open as long as it is not closed, so a "tail -f" style
	 * log viewer is possible
	 * 
	 * @param appName
	 * @param processName
	 * @return the log InputStream
	 * @throws HerokuServiceException
	 */
	public InputStream getProcessLogStream(String appName, String processName) throws HerokuServiceException;

	/**
	 * Restarts the given App
	 * @param app
	 * @throws HerokuServiceException
	 */
	public void restart(App app) throws HerokuServiceException;

	/**
	 * Destroys the given App
	 * @param app
	 * @throws HerokuServiceException
	 */
	public void destroyApp(App app) throws HerokuServiceException;

	/**
	 * Delivers the given App's collaborators
	 * @param app
	 * @return a list containing the App's collaborators
	 * @throws HerokuServiceException
	 */
	public List<Collaborator> getCollaborators(App app) throws HerokuServiceException;

	/**
	 * Adds the given collaborator to the given App
	 * @param app
	 * @param email 
	 * @throws HerokuServiceException
	 */
	public void addCollaborator(App app, String email) throws HerokuServiceException;

	/**
	 * Removes the given collaborator from the given App
	 * @param app
	 * @param email 
	 * @throws HerokuServiceException
	 */
	public void removeCollaborator(App app, String email) throws HerokuServiceException;

	/**
	 * Transfers the given App to a new owner
	 * @param app
	 * @param newOwner
	 * 			the email address of the new owner
	 * @throws HerokuServiceException
	 */
	public void transferApplication(App app, String newOwner) throws HerokuServiceException;

	/**
	 * List all processes for the given app
	 * @param app
	 * @return the list of found processes
	 * @throws HerokuServiceException
	 */
	public List<Proc> listProcesses(App app) throws HerokuServiceException;

	/**
	 * Restarts the given individual process
	 * 
	 * @param proc
	 * @throws HerokuServiceException
	 */
	public void restart(Proc proc) throws HerokuServiceException;

	/**
	 * Restarts all processes with the same dyno name for the procs App
	 * 
	 * @param proc
	 * @throws HerokuServiceException
	 */
	public void restartDyno(HerokuDyno proc) throws HerokuServiceException;

	/**
	 * Checks if an App with the given name already exists.
	 * 
	 * @param appName
	 * @return <code>true</code> if the name already exists, <code>false</code>
	 *         if the name is available
	 * @throws HerokuServiceException
	 */
	public boolean appNameExists(String appName) throws HerokuServiceException;

	/**
	 * Adds a Map of environment variables
	 * 
	 * @param appName
	 * @param envMap
	 *            Map of key-value environment variables pairs
	 * @throws HerokuServiceException
	 */
	public void addEnvVariables(String appName, Map<String, String> envMap) throws HerokuServiceException;

	/**
	 * Lists the environment variables
	 * 
	 * @param appName
	 * @return a Map consisting of key-value environment variable pairs
	 * @throws HerokuServiceException
	 */
	public Map<String, String> listEnvVariables(String appName) throws HerokuServiceException;

	/**
	 * Remove an environment variable from the given app
	 * 
	 * @param appName
	 * @param envKey
	 *            the key of the environment variable to remove
	 * @throws HerokuServiceException
	 */
	public void removeEnvVariable(String appName, String envKey) throws HerokuServiceException;

	/**
	 * Scales the given process type to the given quantity
	 * 
	 * @param appName
	 * @param processType
	 * @param quantity
	 * @throws HerokuServiceException
	 */
	public void scaleProcess(String appName, String processType, int quantity) throws HerokuServiceException;
}
