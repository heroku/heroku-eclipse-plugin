package com.heroku.eclipse.core.services.rest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.LoginFailedException;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuProperties;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;

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

	//	private static final String PREF_API_KEY = "apiKey"; //$NON-NLS-1$
	//	private static final String PREF_SSH_KEY = "sshKey"; //$NON-NLS-1$
	//
	private EventAdmin eventAdmin;
	private RepositoryUtil egitUtils;

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
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PreferenceConstants.P_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR, "unable to access secure store", null); //$NON-NLS-1$
		}

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

	public void setSSHKey(String sshKey) throws HerokuServiceException {
		try {
			IEclipsePreferences p = getPreferences();
			if (sshKey == null || sshKey.trim().isEmpty()) {
				p.remove(PreferenceConstants.P_SSH_KEY);
			}
			else if (!sshKey.equals(getSSHKey())) {
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
			catch (IllegalArgumentException e) {
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

		// ensure that we have valid prefs
		String sshKey = null;
		try {
			getOrCreateHerokuSession();
			sshKey = getSSHKey();

			if (sshKey == null || sshKey.trim().isEmpty()) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_PREFERENCES, "Heroku preferences missing or invalid!"); //$NON-NLS-1$
			}
		}
		catch (HerokuServiceException e) {
			// hide "no api key" behind "invalid preferences"
			if (e.getErrorCode() == HerokuServiceException.NO_API_KEY) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_PREFERENCES, "Heroku preferences missing or invalid!", e); //$NON-NLS-1$
			}
			else {
				throw e;
			}
		}

		return isReady;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.heroku.eclipse.core.services.HerokuServices#listTemplates()
	 */
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
	public App createAppFromTemplate(String appName, String templateName) throws HerokuServiceException {
		try {
			Activator.getDefault().getLogger().log(LogService.LOG_INFO, "creating new Heroku App '"+appName+"' from template '"+templateName+"'" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			App randomApp = getOrCreateHerokuSession().cloneTemplate(templateName);
			getOrCreateHerokuSession().renameApp(randomApp.getName(), appName);
			return getOrCreateHerokuSession().getApp(appName);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public boolean materializeGitApp(App app, IProgressMonitor pm) throws HerokuServiceException {
		boolean rv = false;

		// TODO fetch from egit prefs
		String gitLocation = "/home/udo/git/" + app.getName();

		Activator.getDefault().getLogger().log(LogService.LOG_INFO, "materializing Heroku App '"+app.getName()+"' in workspace" ); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			URIish uri = new URIish(app.getGitUrl());

			final File workdir = new File(gitLocation);

			boolean created = workdir.exists();
			if (!created) {
				created = workdir.mkdirs();
			}

			if (!created || !workdir.isDirectory()) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_LOCAL_GIT_LOCATION, "local Git location is invalid: " + gitLocation); //$NON-NLS-1$
			}

			// TODO: timeout from egit prefs
			int timeout = 5000;
			// int timeout = Activator.getDefault().getPreferenceStore()
			// .getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);

			CloneOperation cloneOp = new CloneOperation(uri, true, null, workdir,
					HerokuProperties.getString("heroku.eclipse.git.defaultRefs"), HerokuProperties.getString("heroku.eclipse.git.defaultOrigin"), timeout); //$NON-NLS-1$ //$NON-NLS-2$
			UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(HerokuProperties.getString("heroku.eclipse.git.defaultUser"), ""); //$NON-NLS-1$ //$NON-NLS-2$
			cloneOp.setCredentialsProvider(user);
			cloneOp.setCloneSubmodules(true);
			runAsJob(uri, cloneOp, app);

			rv = true;
		}
		catch (JGitInternalException e) {
			e.printStackTrace();
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return rv;
	}

	private void runAsJob(final URIish uri, final CloneOperation op, final App app) {
		final Job job = new Job("KASPERL TALKING!") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					IStatus status = executeCloneOperation(op, monitor);

					if (status.isOK()) {
						// enableNatureAction
						return createProject(app.getName(), op.getGitDir().getParentFile().getAbsolutePath(), op.getGitDir(), monitor);
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

	public IStatus createProject(final String projectName, final String projectPath, final File repoDir, IProgressMonitor pm) {
		try {
			IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
				public void run(IProgressMonitor actMonitor) throws CoreException {
					final IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
					desc.setLocation(new Path(projectPath));
					IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(desc.getName());
					prj.create(desc, actMonitor);
					prj.open(actMonitor);
					ConnectProviderOperation cpo = new ConnectProviderOperation(prj, repoDir);
					cpo.execute(new NullProgressMonitor());

					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_ONE, actMonitor);

					IFile pom = prj.getFile("pom.xml");
					// add maven nature, if this is a maven project
					if (pom.exists()) {
						Activator.getDefault().getLogger().log(LogService.LOG_INFO, "Detected Java Maven application" ); //$NON-NLS-1$
//						Job job = new Job("here comes maven") {
//
//							protected IStatus run(IProgressMonitor monitor) {
//								try {
//									ResolverConfiguration configuration = new ResolverConfiguration();
//									configuration.setResolveWorkspaceProjects(workspaceProjects);
//									configuration.setSelectedProfiles(""); //$NON-NLS-1$
//
//									boolean hasMavenNature = project.hasNature(IMavenConstants.NATURE_ID);
//
//									IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
//
//									configurationManager.enableMavenNature(project, configuration, monitor);
//
//									if (!hasMavenNature) {
//										configurationManager.updateProjectConfiguration(project, monitor);
//									}
//								}
//								catch (CoreException ex) {
//									log.error(ex.getMessage(), ex);
//								}
//								return Status.OK_STATUS;
//							}
//						};
					}
					Activator.getDefault().getLogger().log(LogService.LOG_INFO, "Heroku application import completed" ); //$NON-NLS-1$
					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_ONE, actMonitor);
				}
			};
			ResourcesPlugin.getWorkspace().run(wsr, pm);
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		return Status.OK_STATUS;
	}

	// private void importProjects(final Repository repository) {
	// String repoName = getEgitUtils().getRepositoryName(repository);
	// Job importJob = new Job("kasperl importing") {
	//
	// protected IStatus run(IProgressMonitor monitor) {
	// List<File> files = new ArrayList<File>();
	// // ProjectUtil.findProjectFiles(files, repository.getWorkTree(), null,
	// monitor);
	// // if (files.isEmpty())
	// // return Status.OK_STATUS;
	//
	// Set<ProjectRecord> records = new LinkedHashSet<ProjectRecord>();
	// for (File file : files)
	// records.add(new ProjectRecord(file));
	// try {
	// ProjectUtils.createProjects(records, repository, null, monitor);
	// }
	// catch (InvocationTargetException e) {
	// Activator.logError(e.getLocalizedMessage(), e);
	// }
	// catch (InterruptedException e) {
	// Activator.logError(e.getLocalizedMessage(), e);
	// }
	// return Status.OK_STATUS;
	// }
	// };
	// importJob.schedule();
	// }

	public boolean materializeJGitApp(App app) throws HerokuServiceException {
		boolean rv = false;
		String gitLocation = "/home/udo/git/" + app.getName();

		System.err.println("materializing " + app.getName() + ", id " + app.getId());
		try {
			URIish uri = new URIish(app.getGitUrl());

			final File workdir = new File(gitLocation);
			//
			// boolean created = workdir.exists();
			// if (!created) {
			// created = workdir.mkdirs();
			// }
			//
			// if (!created || !workdir.isDirectory()) {
			//				throw new HerokuServiceException(HerokuServiceException.INVALID_LOCAL_GIT_LOCATION, "local Git location is invalid: "+gitLocation ); //$NON-NLS-1$
			// }

			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.setGitDir(workdir).readEnvironment().findGitDir().build();

			Git git = new Git(repository);
			System.err.println("about to clone " + app.getGitUrl() + " into dir " + gitLocation); //$NON-NLS-1$//$NON-NLS-2$

			CloneCommand clone = Git.cloneRepository();
			clone.setBare(false);
			clone.setCloneAllBranches(true);
			clone.setDirectory(workdir).setURI(app.getGitUrl());
			UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider("git", "");
			clone.setCredentialsProvider(user);
			clone.call();

			// // refs/head/master=393838383838
			// final Ref ref = cloneDestination.getInitialBranch();
			//
			// final String remoteName = "origin";
			//
			// boolean created = workdir.exists();
			// if (!created) {
			// created = workdir.mkdirs();
			// }
			//
			// if (!created || !workdir.isDirectory()) {
			//				throw new HerokuServiceException(HerokuServiceException.INVALID_LOCAL_GIT_LOCATION, "local Git location is invalid: "+gitLocation ); //$NON-NLS-1$
			// return false;
			// }
			//
			// int timeout = 5000;
			// CloneOperation clone = new CloneOperation(uri, true,
			// selectedBranches, workdir, ref.getName(), remoteName, timeout);
			//
			// int timeout = Activator.getDefault().getPreferenceStore()
			// .getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			// final CloneOperation op = new CloneOperation(uri, allSelected,
			// selectedBranches, workdir, ref != null ? ref.getName() : null,
			// remoteName, timeout);
			// if (credentials != null)
			// op.setCredentialsProvider(new
			// UsernamePasswordCredentialsProvider(
			// credentials.getUser(), credentials.getPassword()));
			// op.setCloneSubmodules(cloneDestination.isCloneSubmodules());

			rv = true;
		}
		catch (JGitInternalException e) {
			e.printStackTrace();
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rv;
	}

	private RepositoryUtil getEgitUtils() {
		if (egitUtils == null) {
			egitUtils = org.eclipse.egit.core.Activator.getDefault().getRepositoryUtil();
		}

		return egitUtils;
	}
}
