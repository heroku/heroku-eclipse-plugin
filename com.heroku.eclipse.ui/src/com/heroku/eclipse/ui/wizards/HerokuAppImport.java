package com.heroku.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuProperties;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuServices.IMPORT_TYPES;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.git.HerokuCredentialsProvider;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * Import wizard for existing Heroku apps
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppImport extends Wizard implements IImportWizard {
	private HerokuAppImportWizardPage listPage;

	private HerokuServices service;

	private HerokuAppProjectTypePage projectTypePage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	/**
	 * 
	 */
	public HerokuAppImport() {
		service = Activator.getDefault().getService();
	}

	@Override
	public void addPages() {
		setNeedsProgressMonitor(false);

		if (HerokuUtils.verifyPreferences(service, Display.getCurrent().getActiveShell())) {
			try {
				listPage = new HerokuAppImportWizardPage();
				addPage(listPage);
				projectTypePage = new HerokuAppProjectTypePage();
				addPage(projectTypePage);
			}
			catch (Exception e) {
				HerokuUtils.internalError(Display.getCurrent().getActiveShell(), e);
			}
		}
		else {
			Display.getDefault().getActiveShell().close();
		}
	}

	@Override
	public boolean performFinish() {
		final App app = listPage.getSelectedApp();
		final IMPORT_TYPES importType = projectTypePage.getImportType();

		if (app != null) {
			final String destinationDir = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
			final int timeout = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			final HerokuCredentialsProvider cred = new HerokuCredentialsProvider(HerokuProperties.getString("heroku.eclipse.git.defaultUser"), ""); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						IProject existingProject = null;
						try {
							if (importType == IMPORT_TYPES.NEW_PROJECT_WIZARD) {
								final List<IProject> previousProjects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
								PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
									public void run() {
										new NewProjectAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow()).run();
									}
								});

								IProject[] currentProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
								for (IProject current : currentProjects) {
									if (!previousProjects.contains(current)) {
										existingProject = current;
										break;
									}
								}

								if (existingProject == null) {
									throw new HerokuServiceException(HerokuServiceException.INSUFFICIENT_DATA, "new project wizard has been aborted"); //$NON-NLS-1$
								}
							}

							service.materializeGitApp(app, importType, existingProject, destinationDir, timeout,
									Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), cred, monitor); //$NON-NLS-1$
						}
						catch (HerokuServiceException e) {
							// clean up previously created "new project"
							if (importType == IMPORT_TYPES.NEW_PROJECT_WIZARD && existingProject !=null) {
								try {
									existingProject.delete(true, true, monitor);
								}
								catch (CoreException e1) {
									throw new InvocationTargetException(e);
								}
							}
							throw new InvocationTargetException(e);
						}
					}
				});
			}
			catch (InvocationTargetException e) {
				if (e.getCause() instanceof HerokuServiceException) {
					HerokuServiceException e1 = (HerokuServiceException) e.getCause();
					if (e1.getErrorCode() == HerokuServiceException.INVALID_LOCAL_GIT_LOCATION) {
						HerokuUtils
								.userError(
										getShell(),
										Messages.getString("HerokuAppCreateNamePage_Error_GitLocationInvalid_Title"), Messages.getFormattedString("HerokuAppCreateNamePage_Error_GitLocationInvalid", destinationDir + System.getProperty("file.separator") + app.getName())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					}
					else if (e1.getErrorCode() == HerokuServiceException.INSUFFICIENT_DATA) {
						// the "New Project Wizard" has been cancelled
						return false;
					}
					else {
						Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
						HerokuUtils.herokuError(getShell(), e);
					}
				}
				else {
					HerokuUtils.herokuError(getShell(), e);
				}
			}
			catch (InterruptedException e) {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
				HerokuUtils.internalError(getShell(), e);
			}
		}
		return true;
	}

	public boolean canFinish() {
		return !listPage.isCurrentPage();
	}


	/**
	 * @return delivers the selected App from the app list page 
	 */
	public App getActiveApp() {
		return listPage.getSelectedApp();
	}

}
