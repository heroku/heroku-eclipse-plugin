package com.heroku.eclipse.core.services;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.User;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.core.services.model.HerokuProc;
import com.heroku.eclipse.core.services.model.KeyValue;

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
	public static final String TOPIC_SESSION = ROOT_CORE_TOPIC + "session/"; //$NON-NLS-1$

	/**
	 * Base topic for all session related events
	 */
	public static final String TOPIC_APPLICATION = ROOT_CORE_TOPIC + "application/"; //$NON-NLS-1$

	/**
	 * Event topic fired if a session is invalidated
	 * 
	 * @see #KEY_SESSION_INSTANCE
	 */
	public static final String TOPIC_SESSION_INVALID = TOPIC_SESSION + "invalid"; //$NON-NLS-1$
	/**
	 * Event topic fired if a session a new session is created
	 * 
	 * @see #KEY_SESSION_INSTANCE
	 */
	public static final String TOPIC_SESSION_CREATED = TOPIC_SESSION + "created"; //$NON-NLS-1$

	/**
	 * Event topic fired if a new application is successfully created
	 */
	public static final String TOPIC_APPLICATION_NEW = TOPIC_APPLICATION + "new"; //$NON-NLS-1$

	/**
	 * Event topic fired if an existing application is renamed
	 */
	public static final String TOPIC_APPLICATION_RENAMED = TOPIC_APPLICATION + "renamed"; //$NON-NLS-1$

	/**
	 * Event topic fired if an application is transfered to another user
	 */
	public static final String TOPIC_APPLICATION_TRANSFERED = TOPIC_APPLICATION + "transfered"; //$NON-NLS-1$

	/**
	 * Event topic fired if an application is destroyed
	 */
	public static final String TOPIC_APPLICATION_DESTROYED = TOPIC_APPLICATION + "destroyed"; //$NON-NLS-1$

	/**
	 * Base topic fired if a collaborators of an applications are changed
	 */
	public static final String TOPIC_APPLICATION_COLLABORATORS = TOPIC_APPLICATION + "collaborators/"; //$NON-NLS-1$

	/**
	 * Event topic fired after collaborators are added to an application
	 */
	public static final String TOPIC_APPLICATION_COLLABORATORS_ADDED = TOPIC_APPLICATION_COLLABORATORS + "added"; //$NON-NLS-1$

	/**
	 * Event topic fired after collaborators are removed from an application
	 */
	public static final String TOPIC_APPLICATION_COLLABORATORS_REMOVED = TOPIC_APPLICATION_COLLABORATORS + "removed"; //$NON-NLS-1$

	/**
	 * Key used for all widgets of the project
	 */
	public static final String ROOT_WIDGET_ID = "com.heroku.eclipse.identifier"; //$NON-NLS-1$
	
	/**
	 * HashMap containing references to all open log threads
	 */
	static Map<String, Thread> logThreads = Collections.synchronizedMap(new HashMap<String, Thread>());

	/**
	 * Keys for various events, ie when the session has been modified, when an app has been created.
	 * 
	 * @see #TOPIC_SESSION_INVALID
	 * @see #TOPIC_SESSION_CREATED
	 */
	public static final String KEY_SESSION_INSTANCE = "session"; //$NON-NLS-1$

	public static final String KEY_APPLICATION_ID = "applicationId"; //$NON-NLS-1$

	public static final String KEY_APPLICATION_OWNER = "applicationOwner"; //$NON-NLS-1$

	public static final String KEY_COLLABORATORS_LIST = "collaborators"; //$NON-NLS-1$

	public static final String KEY_APPLICATION_NAME = "applicationName"; //$NON-NLS-1$

	/**
	 * Enum representing some fields of an App
	 */
	public static enum APP_FIELDS {
		APP_NAME, APP_GIT_URL, APP_WEB_URL
	}

	/**
	 * Enum representing the various import types for existing projects
	 */
	public static enum IMPORT_TYPES {
		AUTODETECT, NEW_PROJECT_WIZARD, GENERAL_PROJECT, MAVEN, PLAY
	}

	/**
	 * Logs into the Heroku account and if successful, returns the user's
	 * associated API key. Invokes HerokuAPI.obtainApiKey
	 * @param pm 
	 * @param username
	 * @param password
	 * @return the Heroku API key
	 * @throws HerokuServiceException
	 */
	public String obtainAPIKey(IProgressMonitor pm, String username, String password) throws HerokuServiceException;

	/**
	 * Sets the Heroku API key to use for further service calls and stores it in
	 * the global eclipse preferences. If there's an active session, it is
	 * invalidated in case the key changed.
	 * 
	 * <p>
	 * The API key is validated before stored
	 * </p>
	 * @param pm 
	 * @param apiKey
	 *            the Heroku API key, might be <code>null</code> to reset it
	 * @throws HerokuServiceException
	 *             if storage of the key fails or api key is invalid
	 * @see HerokuServices#TOPIC_SESSION_INVALID
	 */
	public void setAPIKey(IProgressMonitor pm, String apiKey) throws HerokuServiceException;

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
	 * @param pm 
	 * @param sshKey
	 *            the SSH key, might be <code>null</code> to reset it
	 * @throws HerokuServiceException
	 *             if storage of the SSH key fails
	 */
	public void setSSHKey(IProgressMonitor pm, String sshKey) throws HerokuServiceException;

	/**
	 * Returns an existing heroku session of the currently set API key or
	 * creates a new one in case there's none created yet
	 * @param pm 
	 * @return a valid heroku session for the currently configured API key
	 * @throws HerokuServiceException
	 *             if the session could not be created (e.g. because of a
	 *             missing API key)
	 * @see HerokuServices#TOPIC_SESSION_CREATED
	 */
	public HerokuSession getOrCreateHerokuSession(IProgressMonitor pm) throws HerokuServiceException;

	/**
	 * Validates the API key
	 * @param pm 
	 * @param apiKey
	 *            the API key to validate. Set <code>null</code> / empty string
	 *            to remove it
	 * @throws HerokuServiceException
	 *             if the key is invalid
	 */
	public void validateAPIKey(IProgressMonitor pm, String apiKey) throws HerokuServiceException;

	/**
	 * Validates if the given SSH public key is well formated
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
	 * @param pm 
	 * @param sshKey
	 * @throws HerokuServiceException
	 */
	public void removeSSHKey(IProgressMonitor pm, String sshKey) throws HerokuServiceException;

	/**
	 * Delivers the list of Apps registered for the currently active API key
	 * @param pm 
	 * @return the list of Apps
	 * @throws HerokuServiceException
	 */
	public List<App> listApps(IProgressMonitor pm) throws HerokuServiceException;

	/**
	 * Determines if "everything" is ready to communicate with Heroku, eg. all
	 * the required preferences have been set up
	 * @param pm 
	 * @return true of false
	 * @throws HerokuServiceException
	 */
	public boolean isReady(IProgressMonitor pm) throws HerokuServiceException;

	/**
	 * Delivers the available Heroku App templates
	 * @param pm 
	 * @return the list of found application templates, may be empty, if none
	 *         were found
	 * @throws HerokuServiceException
	 */
	public List<AppTemplate> listTemplates(IProgressMonitor pm) throws HerokuServiceException;

	/**
	 * Creates the named app from the given template. It does so by first
	 * cloning the template and then renaming the newly created, randomly named
	 * App to its wanted name.
	 * 
	 * @param appName
	 * @param templateName
	 * @param pm
	 *            the progress monitor
	 * @return the newly created App
	 * @throws HerokuServiceException
	 *             if an app with the same name already exists in the user's
	 *             account if the template name is invalid if there are network
	 *             problems
	 */
	public App createAppFromTemplate(IProgressMonitor pm, String appName, String templateName) throws HerokuServiceException;

	/**
	 * Materializes the given app in the user's local git repository and in the
	 * workspace. If an existing Eclipse project is given, the materialized git
	 * checkout will be connected to this Eclipse project.
	 * 
	 * @param pm
	 *            the progress monitor to use
	 * @param app
	 *            the App instance to materialize
	 * @param importType
	 *            one import type out of the IMPORT_TYPES enum
	 * @param existingProject
	 *            an existing Eclipse project or null, if a new project is to be
	 *            used
	 * @param workingDir
	 *            the directory where the project will be materialized
	 * @param timeout
	 * @param progressTitle
	 *            the dialog title to display during the materialization process
	 * @param cred
	 *            the CredentialsProvider containing everything we need to
	 *            authenticate
	 * @return true, if the materialization was successful, otherwise false the
	 *         App instance to materialize
	 * @throws HerokuServiceException
	 */
	public boolean materializeGitApp(IProgressMonitor pm, App app, IMPORT_TYPES importType, IProject existingProject, String workingDir, int timeout, String progressTitle,
			CredentialsProvider cred) throws HerokuServiceException;

	/**
	 * Restart an application
	 * @param pm 
	 * @param app
	 *            the application to restart
	 * @throws HerokuServiceException
	 */
	public void restartApplication(IProgressMonitor pm, App app) throws HerokuServiceException;

	/**
	 * Destroy an application
	 * @param pm 
	 * @param app
	 *            the application to destroy
	 * @throws HerokuServiceException
	 *             if the user lacks rights if "anything else" goes wrong
	 */
	public void destroyApplication(IProgressMonitor pm, App app) throws HerokuServiceException;

	/**
	 * Rename an application
	 * @param pm 
	 * @param application
	 *            the application
	 * @param newName
	 *            the new name
	 * @throws HerokuServiceException
	 */
	public void renameApp(IProgressMonitor pm, App application, String newName) throws HerokuServiceException;

	/**
	 * Retrieves all registered collaborators for the given Heroku App
	 * @param pm 
	 * @param app
	 * @return the list of collaborators
	 * @throws HerokuServiceException
	 */
	public List<Collaborator> getCollaborators(IProgressMonitor pm, App app) throws HerokuServiceException;

	/**
	 * Adds a collaborator to the given Heroku App
	 * @param pm 
	 * @param app
	 * @param email
	 * @throws HerokuServiceException
	 */
	public void addCollaborator(IProgressMonitor pm, App app, String email) throws HerokuServiceException;

	/**
	 * Removes one or more collaborators from an App
	 * @param pm 
	 * @param app
	 * @param email
	 *            a variable length String array
	 * @throws HerokuServiceException
	 */
	public void removeCollaborators(IProgressMonitor pm, App app, String... email) throws HerokuServiceException;

	/**
	 * Transfers the given app to a new owner, identified by his/her dmail
	 * address
	 * @param pm 
	 * @param app
	 * @param newOwner
	 * @throws HerokuServiceException
	 */
	public void transferApplication(IProgressMonitor pm, App app, String newOwner) throws HerokuServiceException;

	/**
	 * Get all processes of an app
	 * @param pm 
	 * @param app
	 * @return all processes
	 * @throws HerokuServiceException
	 */
	public List<HerokuProc> listProcesses(IProgressMonitor pm, App app) throws HerokuServiceException;

	/**
	 * Get application with given name
	 * @param pm 
	 * @param appName
	 *            the application name
	 * @return the app
	 * @throws HerokuServiceException
	 */
	public App getApp(IProgressMonitor pm, String appName) throws HerokuServiceException;

	/**
	 * Determines if the given app is owned by the currently logged in user
	 * @param pm 
	 * @param app
	 *            the App to investigate
	 * @return the outcome ...
	 * @throws HerokuServiceException
	 */
	public boolean isOwnApp(IProgressMonitor pm, App app) throws HerokuServiceException;

	/**
	 * Delivers information about the currently logged in user
	 * @param pm 
	 * @return the Heroku user info
	 * @throws HerokuServiceException
	 */
	public User getUserInfo(IProgressMonitor pm) throws HerokuServiceException;

	/**
	 * Delivers the log stream for the given App.
	 * 
	 * The stream remains open as long as it is not closed, so a "tail -f" style
	 * log viewer is possible
	 * @param pm 
	 * @param appName
	 * @return the log InputStream
	 * @throws HerokuServiceException
	 */
	public InputStream getApplicationLogStream(IProgressMonitor pm, String appName) throws HerokuServiceException;

	/**
	 * Delivers the log stream for the given process of the given App.
	 * 
	 * The stream remains open as long as it is not closed, so a "tail -f" style
	 * log viewer is possible
	 * @param pm 
	 * @param appName
	 * @param processName
	 * @return the log InputStream
	 * @throws HerokuServiceException
	 */
	public InputStream getProcessLogStream(IProgressMonitor pm, String appName, String processName) throws HerokuServiceException;

	/**
	 * Adds a Map of environment variables
	 * @param pm 
	 * @param app
	 * @param envMap
	 *            a KeyValue list of environment variables
	 * @throws HerokuServiceException
	 */
	public void addEnvVariables(IProgressMonitor pm, App app, List<KeyValue> envMap) throws HerokuServiceException;

	/**
	 * Lists the environment variables
	 * @param pm 
	 * @param app
	 * @return a Map consisting of key-value environment variable pairs
	 * @throws HerokuServiceException
	 */
	public List<KeyValue> listEnvVariables(IProgressMonitor pm, App app) throws HerokuServiceException;

	/**
	 * Remove an environment variable from the given app
	 * @param pm 
	 * @param app
	 * @param envKey
	 *            the key of the environment variable to remove
	 * @throws HerokuServiceException
	 */
	public void removeEnvVariable(IProgressMonitor pm, App app, String envKey) throws HerokuServiceException;

	/**
	 * <p>
	 * Performs some <b>basic</b> checks if an App name is valid.
	 * </p>
	 * <p>Currently the following checks are performed:
	 * <ul>
	 * <li>must start with a letter</li>
	 * <li>letters must be lowercase</li>
	 * <li>must only contain letters, numbers or dashes</li>
	 * </ul>
	 * <p>"Basic" checks means that the checks are done without asking the
	 * HerokuAPI and thus not all actual requirements might be checked.</p>
	 * 
	 * @param appName
	 * @return <code>true</code> if the name is valid, <code>false</code> if not
	 */
	public boolean isAppNameBasicallyValid(String appName);

	/**
	 * <p>
	 * Performs some <b>basic</b> checks if an environment variable name is valid.
	 * </p>
	 * <p>Currently the following checks are performed:
	 * <ul>
	 * <li>must start with a letter</li>
	 * <li>must only contain letters, numbers or underscores</li>
	 * </ul>
	 * <p>"Basic" checks means that the checks are done without asking the
	 * HerokuAPI and thus not all actual requirements might be checked.</p>
	 * 
	 * @param envName
	 * @return <code>true</code> if the name is valid, <code>false</code> if not
	 */
	public boolean isEnvNameBasicallyValid(String envName);

	/**
	 * Checks if an App with the given name already exists.
	 * @param pm 
	 * @param appName
	 * @return <code>true</code> if the name already exists, <code>false</code>
	 *         if the name is available
	 * @throws HerokuServiceException
	 */
	public boolean appNameExists(IProgressMonitor pm, String appName) throws HerokuServiceException;

	/**
	 * Delivers the project type for the given App.buildbackProvidedDescription
	 * 
	 * @param buildpackProvidedDescription
	 * @return either the determined element IMPORT_TYPES enum or, per default,
	 *         IMPORT_TYPES.AUTODETECT
	 */
	public IMPORT_TYPES getProjectType(String buildpackProvidedDescription);

	/**
	 * Restarts the given list of Procs
	 * @param pm 
	 * @param procs
	 *            a List of HerokuProc instances
	 * @throws HerokuServiceException
	 */
	public void restartProcs(IProgressMonitor pm, List<HerokuProc> procs) throws HerokuServiceException;

	/**
	 * Restarts all processes with the same dyno name for the given app
	 * @param pm 
	 * @param proc
	 *            the proc providing the dyno name
	 * @throws HerokuServiceException
	 */
	public void restartDyno(IProgressMonitor pm, HerokuProc proc) throws HerokuServiceException;

	/**
	 * Scales the given dyno type to the given quantity
	 * @param pm 
	 * @param appName
	 * @param dynoName
	 * @param quantity
	 * @throws HerokuServiceException
	 */
	public void scaleProcess(IProgressMonitor pm, String appName, String dynoName, int quantity) throws HerokuServiceException;
	
	/**
	 * Delivers the thread responsible for displaying the log for the given App. The thread is either
	 * reused from a previous call or newly created 
	 * @param pm 
	 * @param app 
	 * @param streamCreator 
	 * @param exceptionHandler 
	 */
	public void startAppLogThread(IProgressMonitor pm, App app, LogStreamCreator streamCreator, UncaughtExceptionHandler exceptionHandler);
	
	/**
	 * Delivers the thread responsible for displaying the log for the given HerokuProc. The thread is either
	 * reused from a previous call or newly created 
	 * @param pm 
	 * @param proc
	 * @param streamCreator 
	 * @param exceptionHandler 
	 */
	public void startProcessLogThread(IProgressMonitor pm, HerokuProc proc, LogStreamCreator streamCreator, UncaughtExceptionHandler exceptionHandler);
	
	/**
	 * Delegate interface to wrap the UI MessageConsoleStream into something serviceable.  
	 * @author udo.rader@bestsolution.at
	 */
	interface LogStream {
		/**
		 * @return the state of the stream
		 * @throws IOException 
		 */
		boolean isClosed() throws IOException;
		/**
		 * @throws IOException 
		 */
		void close() throws IOException;
		
		/**
		 * @param buffer
		 * @param i
		 * @param bytesRead
		 * @throws IOException 
		 */
		void write(byte[] buffer, int i, int bytesRead) throws IOException;
	}
	
	/**
	 * Delegate interface intended to wrap the creation of a UI MessageConsoleStream on the service side of life.
	 * @author udo.rader@bestsolution.at
	 */
	interface LogStreamCreator {
		LogStream create();
	}

}


