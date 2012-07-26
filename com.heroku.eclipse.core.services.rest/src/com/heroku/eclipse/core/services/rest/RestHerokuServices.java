package com.heroku.eclipse.core.services.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Proc;
import com.heroku.api.User;
import com.heroku.api.exception.LoginFailedException;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuProperties;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.core.services.model.HerokuDyno;
import com.heroku.eclipse.core.services.model.HerokuProc;
import com.heroku.eclipse.core.services.model.KeyValue;

/**
 * Services class for the Heroclipse plugin, providing access to essential
 * methods of the com.heroku.api.HerokuAPI class
 * 
 * @author udo.rader@bestsolution.at
 */
public class RestHerokuServices implements HerokuServices {
	private RestHerokuSession herokuSession;
	private IEclipsePreferences preferences;
	private ISecurePreferences securePreferences;

	private EventAdmin eventAdmin;
	private RepositoryUtil egitUtils;

	/**
	 * Pattern defining a valid appname
	 */
	private static Pattern VALID_APPNAME = Pattern.compile("^[a-z][a-z0-9-]+$"); //$NON-NLS-1$

	/**
	 * Pattern defining a valid environment variable name
	 */
	private static Pattern VALID_ENVVAR_NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]+$"); //$NON-NLS-1$

	/**
	 * @param eventAdmin
	 */
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * @param eventAdmin
	 */
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}

	@Override
	public String obtainAPIKey(IProgressMonitor pm, final String username, final String password) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<String>() {
			@Override
			public String run() throws HerokuServiceException {
				try {
					String apiKey = HerokuAPI.obtainApiKey(username, password);
					return apiKey;
				}
				catch (LoginFailedException e) {
					Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "Unable to log in to account", e); //$NON-NLS-1$
					throw new HerokuServiceException(HerokuServiceException.LOGIN_FAILED, e);
				}
				catch (Exception e) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to fetch API key", e); //$NON-NLS-1$
					throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
				}
			}
		});
	}

	@Override
	public HerokuSession getOrCreateHerokuSession(IProgressMonitor pm) throws HerokuServiceException {
		String apiKey = getAPIKey();

		if (apiKey == null) {
			throw new HerokuServiceException(HerokuServiceException.NO_API_KEY, "No API-Key configured", null); //$NON-NLS-1$
		}
		else if (herokuSession == null) {
			herokuSession = new RestHerokuSession(apiKey);
			if (eventAdmin != null) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_SESSION_INSTANCE, herokuSession);

				Event event = new Event(TOPIC_SESSION_CREATED, map);
				eventAdmin.postEvent(event);
			}
		}

		return herokuSession;
	}

	@Override
	public String getAPIKey() throws HerokuServiceException {
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PreferenceConstants.P_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR, e);
		}

		return apiKey;
	}

	@Override
	public String getSSHKey() {
		return getPreferences().get(PreferenceConstants.P_SSH_KEY, null);
	}

	@Override
	public void setSSHKey(final IProgressMonitor pm, final String sshKey) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				try {
					IEclipsePreferences p = getPreferences();
					if (sshKey == null || sshKey.trim().isEmpty()) {
						p.remove(PreferenceConstants.P_SSH_KEY);
					}
					else if (!sshKey.equals(getSSHKey()) || ("true".equals(System.getProperty("heroku.devel")))) { //$NON-NLS-1$ //$NON-NLS-2$
						validateSSHKey(sshKey);
						getOrCreateHerokuSession(pm).addSSHKey(sshKey);
						p.put(PreferenceConstants.P_SSH_KEY, sshKey);
					}
					else {
						throw new HerokuServiceException(HerokuServiceException.SSH_KEY_ALREADY_EXISTS, "SSH key already registered with this account!"); //$NON-NLS-1$
					}
					p.flush();
				}
				catch (BackingStoreException e) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to persist preferences", e); //$NON-NLS-1$
					throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
				}
				return new VoidReturn();
			}
		});
	}

	@Override
	public void setAPIKey(final IProgressMonitor pm, final String apiKey) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				try {
					boolean modified = false;
					ISecurePreferences p = getSecurePreferences();
					if (apiKey == null || apiKey.trim().isEmpty()) {
						p.remove(PreferenceConstants.P_API_KEY);
						modified = true;
					}
					else {
						String apiKeyTrimmed = apiKey.trim();
						if (!apiKeyTrimmed.equals(getAPIKey())) {
							validateAPIKey(pm, apiKeyTrimmed);
							p.put(PreferenceConstants.P_API_KEY, apiKeyTrimmed, true);
							modified = true;
						}
					}

					if (modified) {
						p.flush();
						invalidateSession();
					}
				}
				catch (StorageException e) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to access secure preferences", e); //$NON-NLS-1$
					throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR, e);
				}
				catch (IOException e) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to persist secure preferences", e); //$NON-NLS-1$
					throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
				}
				return new VoidReturn();
			}
		});
	}

	@Override
	public void validateAPIKey(final IProgressMonitor pm, final String apiKey) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				try {
					HerokuAPI api = new HerokuAPI(apiKey);
					api.listApps();
				}
				catch (Throwable e) {
					// 401 = invalid API key
					if (e.getClass().equals(RequestFailedException.class)
							&& ((RequestFailedException) e).getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
						throw new HerokuServiceException(HerokuServiceException.INVALID_API_KEY, e);
					}
					else {
						throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
					}
				}
				return new VoidReturn();
			}
		});
	}

	@Override
	public String[] validateSSHKey(String sshKey) throws HerokuServiceException {
		String[] parts = null;
		if (sshKey == null || sshKey.trim().isEmpty()) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, "validation of SSH key failed!"); //$NON-NLS-1$
		}
		else {
			parts = sshKey.split(" "); //$NON-NLS-1$

			if (parts.length != 3) {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "SSH key '" + sshKey + "' is invalid"); //$NON-NLS-1$ //$NON-NLS-2$
				throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, "validation of SSH key failed!"); //$NON-NLS-1$
			}

			try {
				DatatypeConverter.parseBase64Binary(parts[1]);
			}
			catch (Exception e) {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "SSH key '" + sshKey + "' is invalid", e); //$NON-NLS-1$ //$NON-NLS-2$
				throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, "validation of SSH key failed!"); //$NON-NLS-1$
			}
		}

		return parts;
	}

	private void invalidateSession() {
		if (herokuSession != null) {
			herokuSession.invalidate();
			if (eventAdmin != null) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_SESSION_INSTANCE, herokuSession);

				Event event = new Event(TOPIC_SESSION_INVALID, map);
				eventAdmin.postEvent(event);
			}
		}
		herokuSession = null;
	}

	private IEclipsePreferences getPreferences() {
		if (preferences == null) {
			preferences = InstanceScope.INSTANCE.getNode(Activator.ID);
		}
		return preferences;
	}

	private ISecurePreferences getSecurePreferences() {
		if (securePreferences == null) {
			ISecurePreferences root = SecurePreferencesFactory.getDefault();
			securePreferences = root.node(Activator.ID);
		}
		return securePreferences;
	}

	@Override
	public void removeSSHKey(final IProgressMonitor pm, final String sshKey) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				String[] keyParts = validateSSHKey(sshKey);
				getOrCreateHerokuSession(pm).removeSSHKey(keyParts[2]);
				setSSHKey(pm, null);

				return new VoidReturn();
			}
		});
	}

	@Override
	public List<App> listApps(final IProgressMonitor pm) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<List<App>>() {
			@Override
			public List<App> run() throws HerokuServiceException {
				List<App> apps = getOrCreateHerokuSession(pm).listApps();
				return apps;
			}
		});
	}

	@Override
	public boolean isReady(IProgressMonitor pm) throws HerokuServiceException {
		boolean isReady = true;

		if (herokuSession == null) {
			// ensure that we have valid prefs
			String sshKey = null;
			try {
				getOrCreateHerokuSession(pm);
				sshKey = getSSHKey();

				if (sshKey == null || sshKey.trim().isEmpty()) {
					return false;
				}
			}
			catch (HerokuServiceException e) {
				// hide "no api key" behind "invalid preferences"
				if (e.getErrorCode() == HerokuServiceException.NO_API_KEY) {
					return false;
				}
				else {
					throw e;
				}
			}
		}

		return isReady;
	}

	@Override
	public List<AppTemplate> listTemplates(final IProgressMonitor pm) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<List<AppTemplate>>() {
			@Override
			public List<AppTemplate> run() throws HerokuServiceException {
				List<AppTemplate> templates = new ArrayList<AppTemplate>();

				String templateURI = HerokuProperties.getString("heroku.eclipse.templates.URI"); //$NON-NLS-1$

				if (templateURI == null || templateURI.trim().isEmpty()) {
					throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "URI for templates listing is not configured!"); //$NON-NLS-1$
				}
				else {
					ObjectMapper mapper = new ObjectMapper();
					try {
						templates = mapper.readValue(new URL(templateURI), new TypeReference<List<AppTemplate>>() {
						});
					}
					catch (JsonParseException e) {
						Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "unable to parse JSON for templates list", e); //$NON-NLS-1$
						throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "unable to parse JSON templates list", e); //$NON-NLS-1$
					}
					catch (JsonMappingException e) {
						Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "unable to map JSON data from templates list", e); //$NON-NLS-1$
						throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "unable to map JSON data from templates list", e); //$NON-NLS-1$
					}
					catch (MalformedURLException e) {
						Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "malformed URL '" + templateURI + "'for templates listing ", e); //$NON-NLS-1$ //$NON-NLS-2$
						throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "malformed URL '" + templateURI + "'for templates listing ", e); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (IOException e) {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_WARNING, "network error when retrieving templates listing from " + templateURI, e); //$NON-NLS-1$
						throw new HerokuServiceException(HerokuServiceException.REQUEST_FAILED,
								"network error when retrieving templates listing from " + templateURI, e); //$NON-NLS-1$
					}
				}

				return templates;
			}
		});
	}

	@Override
	public App createAppFromTemplate(final IProgressMonitor pm, final String appName, final String templateName) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<App>() {
			@Override
			public App run() throws HerokuServiceException {
				App app = null;
				String nameOnStack = appName;
				if (appName == null || appName.trim().isEmpty()) {
					nameOnStack = "[not defined yet]"; //$NON-NLS-1$
				}
				try {
					if (appName != null && !appName.trim().isEmpty()) {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_INFO, "creating new Heroku App '" + appName + "' from template '" + templateName + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						app = getOrCreateHerokuSession(pm).createAppFromTemplate(new App().named(appName), templateName);
					}
					else {
						Activator.getDefault().getLogger().log(LogService.LOG_INFO, "creating new unnamed Heroku App from template '" + templateName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						app = getOrCreateHerokuSession(pm).createAppFromTemplate(null, templateName);
					}
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(KEY_APPLICATION_ID, app.getId());

					Event event = new Event(TOPIC_APPLICATION_NEW, map);
					eventAdmin.postEvent(event);
				}
				catch (HerokuServiceException e) {
					if (e.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
						destroyApplication(pm, app);
						throw e;
					}
					else {
						Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "unknown error when creating '" + nameOnStack + "', dying ..."); //$NON-NLS-1$ //$NON-NLS-2$
						throw e;
					}
				}

				return app;
			}
		});
	}

	@Override
	public boolean materializeGitApp(IProgressMonitor pm, App app, IMPORT_TYPES importType, IProject existingProject, String gitLocation, int timeout,
			String dialogTitle, CredentialsProvider cred, String transportErrorMessage) throws HerokuServiceException {
		boolean rv = false;

		Activator.getDefault().getLogger().log(LogService.LOG_INFO, "materializing Heroku App '" + app.getName() + "' in workspace, import type " + importType); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			URIish uri = new URIish(app.getGitUrl());

			final File workdir = new File(gitLocation, app.getName());

			boolean created = workdir.exists();
			if (!created) {
				created = workdir.mkdirs();
			}
			// whine if the git location is non empty
			else if (workdir.isDirectory()) {
				String[] entries = workdir.list();
				if (entries != null && entries.length > 0) {
					Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "git location already exists, unable to check out: " + gitLocation); //$NON-NLS-1$
					throw new HerokuServiceException(HerokuServiceException.INVALID_LOCAL_GIT_LOCATION,
							"git location already exists, unable to check out: " + gitLocation); //$NON-NLS-1$
				}
			}

			if (!created || !workdir.isDirectory()) {
				Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "local git location is invalid: " + gitLocation); //$NON-NLS-1$
				throw new HerokuServiceException(HerokuServiceException.INVALID_LOCAL_GIT_LOCATION, "local git location is invalid: " + gitLocation); //$NON-NLS-1$
			}

			CloneOperation cloneOp = new CloneOperation(uri, true, null, workdir,
					HerokuProperties.getString("heroku.eclipse.git.defaultRefs"), HerokuProperties.getString("heroku.eclipse.git.defaultOrigin"), timeout); //$NON-NLS-1$ //$NON-NLS-2$

			cloneOp.setCredentialsProvider(cred);
			cloneOp.setCloneSubmodules(true);
			runCloneJob(uri, cloneOp, app, importType, existingProject, dialogTitle, transportErrorMessage);

			rv = true;
		}
		catch (JGitInternalException e) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
		}
		catch (URISyntaxException e) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
		}

		return rv;
	}

	private void runCloneJob(final URIish uri, final CloneOperation op, final App app, final IMPORT_TYPES importType, final IProject existingProject,
			String dialogTitle, final String transportErrorMessage) {
		final Job job = new Job(dialogTitle) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					IStatus status = executeCloneOperation(monitor, op);

					if (status.isOK()) {
						if (importType == IMPORT_TYPES.NEW_PROJECT_WIZARD && existingProject != null) {
							return createNewEclipseProject(monitor, existingProject, app.getName(), op.getGitDir());
						}
						else {
							return createAutodetectedEclipseProject(monitor, app.getName(), importType, op.getGitDir());
						}
					}
					else {
						return status;
					}
				}
				catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
				catch (InvocationTargetException e) {
					Throwable thr = e.getCause();

					if (thr.getCause() != null) {
						if (thr.getCause() instanceof TransportException) {
							return new Status(IStatus.ERROR, Activator.getPluginId(), 0, transportErrorMessage, thr.getCause());
						}
						else {
							return new Status(IStatus.ERROR, Activator.getPluginId(), 0, thr.getCause().getMessage(), thr.getCause());

						}
					}
					else {
						return new Status(IStatus.ERROR, Activator.getPluginId(), 0, thr.getMessage(), e);
					}
				}
				catch (CoreException e) {
					e.printStackTrace();
					if (importType == IMPORT_TYPES.NEW_PROJECT_WIZARD && existingProject != null) {
						Activator
								.getDefault()
								.getLogger()
								.log(LogService.LOG_ERROR,
										"unknown error when trying to create project '" + existingProject.getName() + "' with new project wizard, aborting ...", e); //$NON-NLS-1$ //$NON-NLS-2$
					}
					else {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_ERROR, "unknown, internal error when trying to create project " + app.getName(), e); //$NON-NLS-1$
					}

					return new Status(IStatus.ERROR, Activator.getPluginId(), 0, e.getMessage(), e);
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private IStatus executeCloneOperation(final IProgressMonitor monitor, final CloneOperation op) throws InvocationTargetException, InterruptedException {
		op.run(monitor);
		getEgitUtils().addConfiguredRepository(op.getGitDir());

		return Status.OK_STATUS;
	}

	/**
	 * Connects the given, already materialized App as an Eclipse project. The
	 * project type is already predefined (autodetected).
	 * 
	 * @param projectName
	 * @param importType
	 *            one import type out of the IMPORT_TYPES enum
	 * @param projectPath
	 * @param repoDir
	 * @param pm
	 *            the progress monitor to use
	 * @return the outcome of the creation process in the form of an IStatus
	 *         object
	 * @throws CoreException
	 * @throws HerokuServiceException
	 */
	private IStatus createAutodetectedEclipseProject(final IProgressMonitor pm, final String projectName, final IMPORT_TYPES importType, final File repoDir)
			throws CoreException {
		final String projectPath = repoDir.getParentFile().getAbsolutePath();
		IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
			@SuppressWarnings("restriction")
			public void run(IProgressMonitor actMonitor) throws CoreException {
				final IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
				desc.setLocation(new Path(projectPath));
				IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(desc.getName());
				newProject.create(desc, actMonitor);
				newProject.open(actMonitor);
				ConnectProviderOperation cpo = new ConnectProviderOperation(newProject, repoDir);
				cpo.execute(actMonitor);

				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_ONE, actMonitor);

				if (importType == IMPORT_TYPES.AUTODETECT || importType == IMPORT_TYPES.MAVEN) {
					IFile pom = newProject.getFile(IMavenConstants.POM_FILE_NAME);
					// add maven nature, if this is a maven project
					if (pom.exists()) {
						Activator.getDefault().getLogger().log(LogService.LOG_INFO, "Detected Java Maven application"); //$NON-NLS-1$
						try {
							ResolverConfiguration configuration = new ResolverConfiguration();
							configuration.setResolveWorkspaceProjects(false);
							//									configuration.setSelectedProfiles(""); //$NON-NLS-1$

							boolean hasMavenNature = newProject.hasNature(IMavenConstants.NATURE_ID);

							IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

							configurationManager.enableMavenNature(newProject, configuration, actMonitor);

							if (!hasMavenNature) {
								configurationManager.updateProjectConfiguration(newProject, actMonitor);
							}
						}
						catch (CoreException e) {
							throw new RuntimeException(e);
						}
					}
				}
				Activator.getDefault().getLogger().log(LogService.LOG_INFO, "Heroku application import completed"); //$NON-NLS-1$
				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_ONE, actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(wsr, pm);

		return Status.OK_STATUS;
	}

	/**
	 * Connects the given, already materialized App as an Eclipse project. The
	 * project type is NOT predefined, instead the "new project wizard" is used.
	 * 
	 * @return the outcome of the creation process in the form of an IStatus
	 *         object
	 * @throws CoreException
	 * @throws HerokuServiceException
	 */
	private IStatus createNewEclipseProject(final IProgressMonitor pm, final IProject eclipseProject, final String herokuName, final File repositoryLocation)
			throws CoreException {
		IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				eclipseProject.open(actMonitor);
				ConnectProviderOperation cpo = new ConnectProviderOperation(eclipseProject, repositoryLocation);
				cpo.execute(actMonitor);
				Activator
						.getDefault()
						.getLogger()
						.log(LogService.LOG_INFO,
								"Heroku application import as a user controlled 'New Project' completed, App name " + herokuName + ", Eclipse name " + eclipseProject.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_ONE, actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(wsr, pm);

		return Status.OK_STATUS;
	}

	private RepositoryUtil getEgitUtils() {
		if (egitUtils == null) {
			egitUtils = org.eclipse.egit.core.Activator.getDefault().getRepositoryUtil();
		}

		return egitUtils;
	}

	@Override
	public void restartApplication(final IProgressMonitor pm, final App app) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).restart(app);
				return new VoidReturn();
			}
		});
	}

	@Override
	public void destroyApplication(final IProgressMonitor pm, final App app) throws HerokuServiceException {
		if (app != null) {
			runCancellableOperation(pm, new RunnableWithReturn<Object>() {
				@Override
				public Object run() throws HerokuServiceException {
					getOrCreateHerokuSession(pm).destroyApp(app);

					Map<String, Object> map = new HashMap<String, Object>();
					map.put(KEY_APPLICATION_ID, app.getId());

					Event event = new Event(TOPIC_APPLICATION_DESTROYED, map);
					eventAdmin.postEvent(event);

					return new VoidReturn();
				}
			});
		}
	}

	@Override
	public void renameApp(final IProgressMonitor pm, final App application, final String newName) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).renameApp(application.getName(), newName);

				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_APPLICATION_ID, application.getId());
				map.put(KEY_APPLICATION_NAME, newName);

				Event event = new Event(TOPIC_APPLICATION_RENAMED, map);
				eventAdmin.postEvent(event);

				return new VoidReturn();
			}
		});
	}

	@Override
	public List<Collaborator> getCollaborators(final IProgressMonitor pm, final App app) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<List<Collaborator>>() {
			@Override
			public List<Collaborator> run() throws HerokuServiceException {
				return getOrCreateHerokuSession(pm).getCollaborators(app);
			}
		});
	}

	@Override
	public void addCollaborator(final IProgressMonitor pm, final App app, final String email) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).addCollaborator(app, email);

				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_APPLICATION_ID, app.getId());
				map.put(KEY_COLLABORATORS_LIST, new String[] { email });

				Event event = new Event(TOPIC_APPLICATION_COLLABORATORS_ADDED, map);
				eventAdmin.postEvent(event);

				return new VoidReturn();
			}
		});
	}

	@Override
	public void removeCollaborators(final IProgressMonitor pm, final App app, final String... emails) throws HerokuServiceException {
		final HerokuSession s = getOrCreateHerokuSession(pm);
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {

				List<String> notremoved = new ArrayList<String>();
				List<String> removed = new ArrayList<String>();

				for (String e : emails) {
					try {
						s.removeCollaborator(app, e);
						removed.add(e);
					}
					catch (HerokuServiceException ex) {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_INFO, "Could not remove collaborator '" + e + "' from application '" + app.getName() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						notremoved.add(e);
					}
				}

				if (!removed.isEmpty()) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(KEY_APPLICATION_ID, app.getId());
					map.put(KEY_COLLABORATORS_LIST, emails);

					Event event = new Event(TOPIC_APPLICATION_COLLABORATORS_ADDED, map);
					eventAdmin.postEvent(event);
				}

				if (!notremoved.isEmpty()) {
					throw new HerokuServiceException(HerokuServiceException.REQUEST_FAILED,
							"one or more collaborators could not be removed: " + notremoved.toString()); //$NON-NLS-1$
				}

				return new VoidReturn();
			}
		});

	}

	@Override
	public void transferApplication(final IProgressMonitor pm, final App app, final String newOwner) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).transferApplication(app, newOwner);

				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_APPLICATION_ID, app.getId());
				map.put(KEY_APPLICATION_OWNER, newOwner);

				Event event = new Event(TOPIC_APPLICATION_TRANSFERED, map);
				eventAdmin.postEvent(event);

				return new VoidReturn();
			}
		});
	}

	@Override
	public List<HerokuProc> listProcesses(final IProgressMonitor pm, final App app) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<List<HerokuProc>>() {
			@Override
			public List<HerokuProc> run() throws HerokuServiceException {
				List<Proc> procs = getOrCreateHerokuSession(pm).listProcesses(app);

				// adding some useful stuff
				List<HerokuProc> convertedProcs = new ArrayList<HerokuProc>();
				for (Proc proc : procs) {
					convertedProcs.add(new HerokuProc(proc));
				}
				return convertedProcs;
			}
		});
	}

	@Override
	public List<HerokuDyno> listDynos(IProgressMonitor pm, App app)
			throws HerokuServiceException {
		Map<String, HerokuDyno> map = new HashMap<String, HerokuDyno>();
		List<HerokuDyno> list = new ArrayList<HerokuDyno>();
		List<HerokuProc> processes = listProcesses(pm, app);
		
		for( HerokuProc p : processes ) {
			HerokuDyno g = map.get(p.getDynoName());
			if( g == null ) {
				g = new HerokuDyno(p.getDynoName(),app.getName(),p.getHerokuProc().getCommand());
				list.add(g);
				map.put(g.getName(), g);
			}
			
			g.add(p);
		}
		
		return list;
	}
	
	@Override
	public App getApp(final IProgressMonitor pm, final String appName) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<App>() {
			@Override
			public App run() throws HerokuServiceException {
				return getOrCreateHerokuSession(pm).getApp(appName);
			}
		});
	}

	@Override
	public boolean isOwnApp(final IProgressMonitor pm, final App app) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<Boolean>() {
			@Override
			public Boolean run() throws HerokuServiceException {
				User user = getOrCreateHerokuSession(pm).getUserInfo();
				if (user != null && user.getEmail().trim().equalsIgnoreCase(app.getOwnerEmail())) {
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public User getUserInfo(final IProgressMonitor pm) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<User>() {
			@Override
			public User run() throws HerokuServiceException {
				return getOrCreateHerokuSession(pm).getUserInfo();
			}
		});
	}

	@Override
	public InputStream getApplicationLogStream(IProgressMonitor pm, String appName) throws HerokuServiceException {
		return getOrCreateHerokuSession(pm).getApplicationLogStream(appName);
	}

	@Override
	public InputStream getProcessLogStream(IProgressMonitor pm, String appName, String processName) throws HerokuServiceException {
		return getOrCreateHerokuSession(pm).getProcessLogStream(appName, processName);
	}

	@Override
	public boolean appNameExists(final IProgressMonitor pm, final String appName) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<Boolean>() {
			@Override
			public Boolean run() throws HerokuServiceException {
				return getOrCreateHerokuSession(pm).appNameExists(appName);
			}
		});
	}

	@Override
	public IMPORT_TYPES getProjectType(String buildpackProvidedDescription) {
		if ("Java".equalsIgnoreCase(buildpackProvidedDescription)) { //$NON-NLS-1$
			return IMPORT_TYPES.MAVEN;
		}
		else if ("Play".equalsIgnoreCase(buildpackProvidedDescription)) { //$NON-NLS-1$
			return IMPORT_TYPES.PLAY;
		}

		return IMPORT_TYPES.AUTODETECT;
	}

	@Override
	public void restartProcs(IProgressMonitor pm, final List<HerokuProc> procs) throws HerokuServiceException {
		final HerokuSession s = getOrCreateHerokuSession(pm);

		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				for (HerokuProc proc : procs) {
					s.restart(proc.getHerokuProc());
				}
				return new VoidReturn();
			}
		});
	}

	@Override
	public void restartDyno(final IProgressMonitor pm, final HerokuDyno dyno) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).restartDyno(dyno);
				return new VoidReturn();
			}
		});
	}

	@Override
	public void addEnvVariables(final IProgressMonitor pm, final App app, final List<KeyValue> envList) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				HashMap<String, String> envMap = new HashMap<String, String>();
				for (KeyValue keyValue : envList) {
					envMap.put(keyValue.getKey(), keyValue.getValue());
				}
				getOrCreateHerokuSession(pm).addEnvVariables(app.getName(), envMap);
				return new VoidReturn();
			}
		});
	}

	@Override
	public List<KeyValue> listEnvVariables(final IProgressMonitor pm, final App app) throws HerokuServiceException {
		return runCancellableOperation(pm, new RunnableWithReturn<List<KeyValue>>() {
			@Override
			public List<KeyValue> run() throws HerokuServiceException {
				List<KeyValue> list = new ArrayList<KeyValue>();
				Map<String, String> map = getOrCreateHerokuSession(pm).listEnvVariables(app.getName());

				for (String key : map.keySet()) {
					list.add(new KeyValue(key, map.get(key)));
				}
				return list;
			}
		});
	}

	@Override
	public void removeEnvVariable(final IProgressMonitor pm, final App app, final String envKey) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).removeEnvVariable(app.getName(), envKey);
				return new VoidReturn();
			}
		});
	}

	@Override
	public boolean isAppNameBasicallyValid(String appName) {
		return VALID_APPNAME.matcher(appName).matches();
	}

	@Override
	public boolean isEnvNameBasicallyValid(String envName) {
		return VALID_ENVVAR_NAME.matcher(envName).matches();
	}

	@Override
	public void scaleProcess(final IProgressMonitor pm, final String appName, final String dynoName, final int quantity) throws HerokuServiceException {
		runCancellableOperation(pm, new RunnableWithReturn<Object>() {
			@Override
			public Object run() throws HerokuServiceException {
				getOrCreateHerokuSession(pm).scaleProcess(appName, dynoName, quantity);
				return new VoidReturn();
			}
		});
	}

	/**
	 * Starts a thread running the given operation that can be interrupted using
	 * the given progress monitor. So users may cancel the given operation using
	 * an ordinary progress monitor.
	 * 
	 * @param pm
	 * @param r
	 * @return
	 * @throws HerokuServiceException
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	private <V> V runCancellableOperation(IProgressMonitor pm, final RunnableWithReturn<V> r) throws HerokuServiceException {
		final AtomicReference<Object> rv = new AtomicReference<Object>();
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					rv.set(r.run());
				}
				catch (Throwable e) {
					rv.set(e);
				}
			}
		};

		t.setDaemon(true);
		t.start();

		while (rv.get() == null) {
			if (pm.isCanceled()) {
				t.stop();
				throw new HerokuServiceException(HerokuServiceException.OPERATION_CANCELLED, "thread interrupted"); //$NON-NLS-1$
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				// nothing to do, just being happy if we are interrupted
			}
		}
		if (rv.get() instanceof HerokuServiceException) {
			throw (HerokuServiceException) rv.get();
		} else if( rv.get() instanceof Throwable ) {
			throw new HerokuServiceException((Throwable) rv.get());
		}

		return (V) rv.get();
	}

	private interface RunnableWithReturn<O> {
		public O run() throws HerokuServiceException;
	}

	/**
	 * Container for void return values of the #runCancellableOperation
	 */
	private static class VoidReturn {
	}

	@Override
	public void startAppLogThread(IProgressMonitor pm, final App app, final LogStreamCreator streamCreator, UncaughtExceptionHandler exceptionHandler) {
		startLogThread(pm, "logstream-app-" + app.getId(), streamCreator, exceptionHandler, app.getName(), null); //$NON-NLS-1$
	}

	@Override
	public void startDynoLogThread(IProgressMonitor pm, HerokuDyno proc, final LogStreamCreator streamCreator, UncaughtExceptionHandler exceptionHandler) {
		startLogThread(pm, "logstream-proc-" + proc.getName(), streamCreator, exceptionHandler, proc.getAppName(), proc.getName()); //$NON-NLS-1$
	}

	/**
	 * Starts a thread connecting a Heroku log stream with a
	 * MessageConsoleStream
	 * 
	 * @param streamName
	 * @param streamCreator
	 */
	private void startLogThread(final IProgressMonitor pm, String streamName, final LogStreamCreator streamCreator, UncaughtExceptionHandler exceptionHandler,
			final String appName, final String procName) {
		// only start new log thread for the given stream if we have not created
		// one before
		if (!logThreads.containsKey(streamName)) {

			Thread t = new Thread(streamName) {
				LogStream out = streamCreator.create();
				AtomicBoolean wantsFun = new AtomicBoolean(true);

				@Override
				public void run() {
					while (wantsFun.get()) {
						byte[] buffer = new byte[1024];
						int bytesRead;
						try {
							InputStream is;
							if (procName == null) {
								is = getApplicationLogStream(pm, appName);
							}
							else {
								is = getProcessLogStream(pm, appName, procName);
							}

							while ((bytesRead = is.read(buffer)) != -1) {
								if (out.isClosed()) {
									break;
								}
								out.write(buffer, 0, bytesRead);
							}
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
						catch (HerokuServiceException e) {
							throw new RuntimeException(e);
						}
					}
				}

				@Override
				public void interrupt() {
					wantsFun.set(false);
					try {
						out.close();
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					super.interrupt();
				}
			};

			t.setUncaughtExceptionHandler(exceptionHandler);
			t.setDaemon(true);
			t.start();

			logThreads.put(streamName, t);
		}
	}

	interface LogThread extends Runnable {
		public void setException(Exception e);

		public Exception getException();
	}

}
