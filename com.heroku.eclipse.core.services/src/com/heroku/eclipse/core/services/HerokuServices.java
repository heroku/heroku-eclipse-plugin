package com.heroku.eclipse.core.services;

import java.util.List;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;

/**
 * Interface defining how Heroclipse talks with the com.heroku.api.HerokuAPI API
 * 
 * @author udo.rader@bestsolution.at
 */
public interface HerokuServices {

	/**
	 * Root topic of all heroku events
	 */
	public static final String ROOT_TOPIC = "com/heroku/eclipse/"; //$NON-NLS-1$

	/**
	 * Root topic of all heroku core events
	 */
	public static final String ROOT_CORE_TOPIC = ROOT_TOPIC + "core/"; //$NON-NLS-1$

	/**
	 * Base topic for all session related events
	 */
	public static final String TOPIC_SESSION = ROOT_CORE_TOPIC + "session/";

	/**
	 * Base topic for all session related events
	 */
	public static final String TOPIC_APPLICATION = ROOT_CORE_TOPIC
			+ "application/";

	/**
	 * Event topic fired if a session is invalidated
	 * 
	 * @see #KEY_SESSION_INSTANCE
	 */
	public static final String TOPIC_SESSION_INVALID = TOPIC_SESSION
			+ "invalid"; //$NON-NLS-1$
	/**
	 * Event topic fired if a session a new session is created
	 * 
	 * @see #KEY_SESSION_INSTANCE
	 */
	public static final String TOPIC_SESSION_CREATED = TOPIC_SESSION
			+ "created"; //$NON-NLS-1$

	/**
	 * Event topic fired if a new application is successfully created
	 */
	public static final String TOPIC_APPLICATION_NEW = TOPIC_APPLICATION
			+ "new";

	/**
	 * Key used for all widgets of the project
	 */
	public static final String ROOT_WIDGET_ID = "com.heroku.eclipse.identifier"; //$NON-NLS-1$

	/**
	 * Event key holding the session modified
	 * 
	 * @see #TOPIC_SESSION_INVALID
	 * @see #TOPIC_SESSION_CREATED
	 */
	public static final String KEY_SESSION_INSTANCE = "session"; //$NON-NLS-1$

	/**
	 * Logs into the Heroku account and if successful, returns the user's
	 * associated API key. Invokes HerokuAPI.obtainApiKey
	 * 
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
	 * Stores the SSH key both in the global eclipse preferences and in the
	 * user's account
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
	 * Validates if the given SSH public key is well formated
	 * 
	 * @param sshKey
	 *            the SSH key to validate
	 * @throws HerokuServiceException
	 *             if the key is invalid
	 * @return a string array consisting of the key parts
	 */
	public String[] validateSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * Removes the given SSH key from both the active Heroku session and the
	 * Eclipse preferences
	 * 
	 * @param sshKey
	 * @throws HerokuServiceException
	 */
	public void removeSSHKey(String sshKey) throws HerokuServiceException;

	/**
	 * Delivers the list of Apps registered for the currently active API key
	 * 
	 * @return the list of Apps
	 * @throws HerokuServiceException
	 */
	public List<App> listApps() throws HerokuServiceException;

	/**
	 * Determines if "everything" is ready to communicate with Heroku, eg. all
	 * the required preferences have been set up
	 * 
	 * @return true of false
	 * @throws HerokuServiceException
	 */
	public boolean isReady() throws HerokuServiceException;

	/**
	 * Delivers the available Heroku App templates
	 * 
	 * @return the list of found application templates, may be empty, if none
	 *         were found
	 * @throws HerokuServiceException
	 */
	public List<AppTemplate> listTemplates() throws HerokuServiceException;

	/**
	 * Creates the named app from the given template. It does so by first
	 * cloning the template and then renaming the newly created, randomly named
	 * App to its wanted name.
	 * 
	 * @param appName
	 * @param templateName
	 * @return the newly created App
	 * @throws HerokuServiceException
	 *             if an app with the same name already exists in the user's
	 *             account if the template name is invalid if there are network
	 *             problems
	 */
	public App createAppFromTemplate(String appName, String templateName)
			throws HerokuServiceException;

	/**
	 * Materializes the given app in the user's local git repository
	 * 
	 * @param app
	 *            the App instance to materialize
	 * @return the materialized App
	 * @throws HerokuServiceException
	 */
	public App materializeGitApp(App app) throws HerokuServiceException;

	/**
	 * Checks if the service is configured so that a session can be created
	 * 
	 * @return <code>true</code> if configured so that a session can be created
	 */
	public boolean canObtainHerokuSession();

	/**
	 * Restart an application
	 * 
	 * @param app
	 *            the application to restart
	 * @throws HerokuServiceException
	 */
	public void restartApplication(App app) throws HerokuServiceException;

	/**
	 * Destroy an application
	 * 
	 * @param app
	 *            the application to destroy
	 * @throws HerokuServiceException
	 */
	public void destroyApplication(App app) throws HerokuServiceException;

	/**
	 * Rename an application
	 * 
	 * @param application
	 *            the application
	 * @param newName
	 *            the new name
	 * @throws HerokuServiceException
	 */
	public void renameApp(App application, String newName)
			throws HerokuServiceException;
}
