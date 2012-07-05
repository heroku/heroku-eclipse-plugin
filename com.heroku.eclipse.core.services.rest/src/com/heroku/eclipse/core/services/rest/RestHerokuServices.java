package com.heroku.eclipse.core.services.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	 * Pattern helpful for determining the real meaning of a HTTP 422 response code, mapped to {@link HerokuServiceException#NOT_ACCEPTABLE}
	 */
	private static Pattern VALID_APPNAME = Pattern.compile("^[a-z][a-z0-9-]+$"); //$NON-NLS-1$

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
	public String obtainAPIKey(String username, String password) throws HerokuServiceException {
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
			throw new HerokuServiceException(e);
		}
	}

	public HerokuSession getOrCreateHerokuSession() throws HerokuServiceException {
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
	public void setSSHKey(String sshKey) throws HerokuServiceException {
		try {
			IEclipsePreferences p = getPreferences();
			if (sshKey == null || sshKey.trim().isEmpty()) {
				p.remove(PreferenceConstants.P_SSH_KEY);
			}
			else if (!sshKey.equals(getSSHKey()) || ("true".equals(System.getProperty("heroku.devel")))) { //$NON-NLS-1$ //$NON-NLS-2$
				validateSSHKey(sshKey);
				getOrCreateHerokuSession().addSSHKey(sshKey);
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
	}

	@Override
	public void setAPIKey(String apiKey) throws HerokuServiceException {
		try {
			boolean modified = false;
			ISecurePreferences p = getSecurePreferences();
			if (apiKey == null || apiKey.trim().isEmpty()) {
				p.remove(PreferenceConstants.P_API_KEY);
				modified = true;
			}
			else {
				apiKey = apiKey.trim();
				if (!apiKey.equals(getAPIKey())) {
					validateAPIKey(apiKey);
					p.put(PreferenceConstants.P_API_KEY, apiKey, true);
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
	}

	public void validateAPIKey(String apiKey) throws HerokuServiceException {
		try {
			HerokuAPI api = new HerokuAPI(apiKey);
			api.listApps();
		}
		catch (Throwable e) {
			// 401 = invalid API key
			if (e.getClass().equals(RequestFailedException.class) && ((RequestFailedException) e).getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_API_KEY, e);
			}
			else {
				throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e);
			}
		}
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
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		String[] keyParts = validateSSHKey(sshKey);
		getOrCreateHerokuSession().removeSSHKey(keyParts[2]);
		setSSHKey(null);
	}

	@Override
	public List<App> listApps() throws HerokuServiceException {
		// = new ArrayList<App>();
		List<App> apps = getOrCreateHerokuSession().listApps();
		return apps;
	}

	@Override
	public boolean isReady() throws HerokuServiceException {
		boolean isReady = true;

		if (herokuSession == null) {
			// ensure that we have valid prefs
			String sshKey = null;
			try {
				getOrCreateHerokuSession();
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
	public List<AppTemplate> listTemplates() throws HerokuServiceException {
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
				Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "network error when retrieving templates listing from " + templateURI, e); //$NON-NLS-1$
				throw new HerokuServiceException(HerokuServiceException.REQUEST_FAILED,
						"network error when retrieving templates listing from " + templateURI, e); //$NON-NLS-1$
			}
		}

		return templates;
	}

	@Override
	public App createAppFromTemplate(String appName, String templateName, IProgressMonitor pm) throws HerokuServiceException {
		App app = null;
		try {
			Activator.getDefault().getLogger().log(LogService.LOG_INFO, "creating new Heroku App '" + appName + "' from template '" + templateName + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			app = getOrCreateHerokuSession().createAppFromTemplate(new App().named(appName), templateName);

			Map<String, Object> map = new HashMap<String, Object>();
			map.put(KEY_APPLICATION_ID, app.getId());

			Event event = new Event(TOPIC_APPLICATION_NEW, map);
			eventAdmin.postEvent(event);
		}
		catch (HerokuServiceException e) {
			// remove dead cloned template
			// TODO: dead code, but removing breaks unit tests ...
			if (app != null && e.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
				destroyApplication(app);
				throw e;
			}
			else {
				Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "unknown error when creating '" + appName + "', dying ..."); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}
		}

		return app;
	}

	@Override
	public boolean materializeGitApp(App app, IMPORT_TYPES importType, IProject existingProject, String gitLocation, int timeout, String dialogTitle,
			CredentialsProvider cred, IProgressMonitor pm) throws HerokuServiceException {
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
			runAsJob(uri, cloneOp, app, importType, existingProject, dialogTitle);

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

	private void runAsJob(final URIish uri, final CloneOperation op, final App app, final IMPORT_TYPES importType, final IProject existingProject,
			String dialogTitle) {
		final Job job = new Job(dialogTitle) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					IStatus status = executeCloneOperation(op, monitor);

					if (status.isOK()) {
						if (importType == IMPORT_TYPES.NEW_PROJECT_WIZARD && existingProject != null) {
							return createNewEclipseProject(existingProject, app.getName(), op.getGitDir(), monitor);
						}
						else {
							return createAutodetectedEclipseProject(app.getName(), importType, op.getGitDir(), monitor);
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
					return new Status(IStatus.ERROR, Activator.getPluginId(), 0, thr.getMessage(), thr);
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

	private IStatus executeCloneOperation(final CloneOperation op, final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
	private IStatus createAutodetectedEclipseProject(final String projectName, final IMPORT_TYPES importType, final File repoDir, final IProgressMonitor pm)
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
						catch (CoreException ex) {
							// TODO: throw ite
							ex.printStackTrace();
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
	private IStatus createNewEclipseProject(final IProject eclipseProject, final String herokuName, final File repositoryLocation, IProgressMonitor pm)
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
	public void restartApplication(App app) throws HerokuServiceException {
		getOrCreateHerokuSession().restart(app);
	}

	@Override
	public void destroyApplication(App app) throws HerokuServiceException {
		getOrCreateHerokuSession().destroyApp(app);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_APPLICATION_ID, app.getId());

		Event event = new Event(TOPIC_APPLICATION_DESTROYED, map);
		eventAdmin.postEvent(event);
	}

	@Override
	public void renameApp(App application, String newName) throws HerokuServiceException {
		getOrCreateHerokuSession().renameApp(application.getName(), newName);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_APPLICATION_ID, application.getId());
		map.put(KEY_APPLICATION_NAME, newName);

		Event event = new Event(TOPIC_APPLICATION_RENAMED, map);
		eventAdmin.postEvent(event);
	}

	public List<Collaborator> getCollaborators(App app) throws HerokuServiceException {
		return getOrCreateHerokuSession().getCollaborators(app);
	}

	@Override
	public void addCollaborator(App app, String email) throws HerokuServiceException {
		getOrCreateHerokuSession().addCollaborator(app, email);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_APPLICATION_ID, app.getId());
		map.put(KEY_COLLABORATORS_LIST, new String[] { email });

		Event event = new Event(TOPIC_APPLICATION_COLLABORATORS_ADDED, map);
		eventAdmin.postEvent(event);
	}

	@Override
	public void removeCollaborators(App app, String... emails) throws HerokuServiceException {
		HerokuSession s = getOrCreateHerokuSession();

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
			throw new HerokuServiceException(HerokuServiceException.REQUEST_FAILED, "one or more collaborators could not be removed: " + notremoved.toString()); //$NON-NLS-1$
		}
	}

	@Override
	public void transferApplication(App app, String newOwner) throws HerokuServiceException {
		getOrCreateHerokuSession().transferApplication(app, newOwner);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_APPLICATION_ID, app.getId());
		map.put(KEY_APPLICATION_OWNER, newOwner);

		Event event = new Event(TOPIC_APPLICATION_TRANSFERED, map);
		eventAdmin.postEvent(event);
	}

	public List<HerokuProc> listProcesses(App app) throws HerokuServiceException {
		List<Proc> procs = getOrCreateHerokuSession().listProcesses(app);

		// adding some useful stuff
		List<HerokuProc> convertedProcs = new ArrayList<HerokuProc>();
		for (Proc proc : procs) {
			convertedProcs.add(new HerokuProc(proc));
		}
		return convertedProcs;
	}

	public App getApp(String appName) throws HerokuServiceException {
		return getOrCreateHerokuSession().getApp(appName);
	}

	@Override
	public boolean isOwnApp(App app) throws HerokuServiceException {
		User user = getOrCreateHerokuSession().getUserInfo();
		if (user != null && user.getEmail().trim().equalsIgnoreCase(app.getOwnerEmail())) {
			return true;
		}
		return false;
	}

	@Override
	public User getUserInfo() throws HerokuServiceException {
		return getOrCreateHerokuSession().getUserInfo();
	}

	@Override
	public InputStream getApplicationLogStream(String appName) throws HerokuServiceException {
		return getOrCreateHerokuSession().getApplicationLogStream(appName);
	}

	@Override
	public InputStream getProcessLogStream(String appName, String processName) throws HerokuServiceException {
		return getOrCreateHerokuSession().getProcessLogStream(appName, processName);
	}

	@Override
	public boolean appNameExists(String appName) throws HerokuServiceException {
		return getOrCreateHerokuSession().appNameExists(appName);
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
	public void restartProcs(List<HerokuProc> procs) throws HerokuServiceException {
		HerokuSession s = getOrCreateHerokuSession();
		for (HerokuProc proc : procs) {
			s.restart(proc.getHerokuProc());
		}
	}

	@Override
	public void restartDyno(HerokuProc dyno) throws HerokuServiceException {
		getOrCreateHerokuSession().restartDyno(dyno);
	}

	@Override
	public void addEnvVariables(App app, Map<String, String> envMap) throws HerokuServiceException {
		getOrCreateHerokuSession().addEnvVariables(app.getName(), envMap);
	}

	@Override
	public List<KeyValue> listEnvVariables(App app) throws HerokuServiceException {
		List<KeyValue> list = new ArrayList<KeyValue>();
		Map<String,String> map = getOrCreateHerokuSession().listEnvVariables(app.getName());
		
		for(String key : map.keySet()) {
			list.add(new KeyValue(key,map.get(key)));
		}
		return list;
	}

	@Override
	public void removeEnvVariable(App app, String envKey) throws HerokuServiceException {
		getOrCreateHerokuSession().removeEnvVariable(app.getName(), envKey);
	}

	@Override
	public void scaleProcess(String appName, String dynoName, int quantity) throws HerokuServiceException {
		getOrCreateHerokuSession().scaleProcess(appName, dynoName, quantity);
	}

	@Override
	public boolean isAppNameBasicallyValid(String appName) throws HerokuServiceException {
		return VALID_APPNAME.matcher(appName).matches();
	}
}
