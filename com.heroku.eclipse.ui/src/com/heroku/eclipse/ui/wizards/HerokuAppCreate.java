package com.heroku.eclipse.ui.wizards;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.ui.Activator;

/**
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuAppCreate extends Wizard implements IImportWizard {
	
	private HerokuAppCreateNamePage namePage;
	private HerokuAppCreateTemplatePage templatePage;
	
	private HerokuServices service;

	/**
	 * 
	 */
	public HerokuAppCreate() {
		service = Activator.getDefault().getService();
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(false);

		try {
			namePage = new HerokuAppCreateNamePage();
			addPage(namePage);
			templatePage = new HerokuAppCreateTemplatePage();
			addPage(templatePage);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performFinish() {
		boolean rv = false;
		
		App app = createHerokuApp();
		// materialize project locally
		if ( app != null ) {
			
			rv = true;
		}
		
//		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("blabla");
//		
//		if ( ! project.exists() ) {
//			try {
//				project.create(null);
//				JavaCore.create(project);
//			}
//			catch (CoreException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
//		MessageDialog.openInformation(getShell(), "hmm", "the larch");
		return rv;
	}

	/**
	 * Creates the app on the Heroku side
	 * @return the newly created App instance
	 */
	private App createHerokuApp() {
		App app = null;
		
		String appName = namePage.getAppName();
		
		if ( appName != null ) {
			AppTemplate template = templatePage.getAppTemplate();
			
			if ( template != null ) {
				try {
					app = service.createAppFromTemplate(appName, template.getTemplateName());
				}
				catch (HerokuServiceException e) {
					e.printStackTrace();
				}
			}
		}
		
		return app;
	}
	
//	private boolean materizalizeApp( App app ) {
//		try {
//			URIish uri = new URIish(app.getGitUrl());
//			setWindowTitle("cloning template into local workspace ...");
//
//			final File workdir = new File("/home/udo/git/");
//			final Ref ref = cloneDestination.getInitialBranch();
//			final String remoteName = cloneDestination.getRemote();
//
//		}
//		catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
//		
//		return false;
//	}
	
//	/**
//	 * Do the clone using data which were collected on the pages
//	 * {@code validSource} and {@code cloneDestination}
//	 *
//	 * @param gitRepositoryInfo
//	 * @return if clone was successful
//	 * @throws URISyntaxException
//	 */
//	protected boolean performClone(GitRepositoryInfo gitRepositoryInfo) throws URISyntaxException {
//		URIish uri = new URIish(gitRepositoryInfo.getCloneUri());
//		UserPasswordCredentials credentials = gitRepositoryInfo.getCredentials();
//		setWindowTitle(NLS.bind(UIText.GitCloneWizard_jobName, uri.toString()));
//		final boolean allSelected;
//		final Collection<Ref> selectedBranches;
//		if (validSource.isSourceRepoEmpty()) {
//			// fetch all branches of empty repo
//			allSelected = true;
//			selectedBranches = Collections.emptyList();
//		} else {
//			allSelected = validSource.isAllSelected();
//			selectedBranches = validSource.getSelectedBranches();
//		}
//		final File workdir = cloneDestination.getDestinationFile();
//		final Ref ref = cloneDestination.getInitialBranch();
//		final String remoteName = cloneDestination.getRemote();
//
//		boolean created = workdir.exists();
//		if (!created)
//			created = workdir.mkdirs();
//
//		if (!created || !workdir.isDirectory()) {
//			final String errorMessage = NLS.bind(
//					UIText.GitCloneWizard_errorCannotCreate, workdir.getPath());
//			ErrorDialog.openError(getShell(), getWindowTitle(),
//					UIText.GitCloneWizard_failed, new Status(IStatus.ERROR,
//							Activator.getPluginId(), 0, errorMessage, null));
//			// let's give user a chance to fix this minor problem
//			return false;
//		}
//
//		int timeout = Activator.getDefault().getPreferenceStore()
//				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
//		final CloneOperation op = new CloneOperation(uri, allSelected,
//				selectedBranches, workdir, ref != null ? ref.getName() : null,
//				remoteName, timeout);
//		if (credentials != null)
//			op.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
//					credentials.getUser(), credentials.getPassword()));
//		op.setCloneSubmodules(cloneDestination.isCloneSubmodules());
//
//		configureFetchSpec(op, gitRepositoryInfo, remoteName);
//		configurePush(op, gitRepositoryInfo, remoteName);
//		configureRepositoryConfig(op, gitRepositoryInfo);
//
//		if (cloneDestination.isImportProjects()) {
//			final IWorkingSet[] sets = cloneDestination.getWorkingSets();
//			op.addPostCloneTask(new PostCloneTask() {
//				public void execute(Repository repository,
//						IProgressMonitor monitor) throws CoreException {
//					importProjects(repository, sets);
//				}
//			});
//		}
//
//		alreadyClonedInto = workdir.getPath();
//
//		if (!callerRunsCloneOperation)
//			runAsJob(uri, op);
//		else
//			cloneOperation = op;
//		return true;
//	}

}
