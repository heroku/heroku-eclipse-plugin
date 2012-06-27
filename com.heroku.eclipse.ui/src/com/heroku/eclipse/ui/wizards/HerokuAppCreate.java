package com.heroku.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuProperties;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.git.HerokuCredentialsProvider;
import com.heroku.eclipse.ui.utils.HerokuUtils;

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
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		setNeedsProgressMonitor(true);

		if ( HerokuUtils.verifyPreferences(service, Display.getCurrent().getActiveShell())) {
			try {
				namePage = new HerokuAppCreateNamePage();
				addPage(namePage);
				templatePage = new HerokuAppCreateTemplatePage();
				addPage(templatePage);
			}
			catch (Exception e) {
				e.printStackTrace();
				HerokuUtils.internalError(Display.getCurrent().getActiveShell(), e);
			}
		}
	}

	@Override
	public boolean performFinish() {
		boolean rv = true;

		final String appName = namePage.getAppName();
		final AppTemplate template = templatePage.getAppTemplate();

		final String destinationDir = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
		final int timeout = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		final HerokuCredentialsProvider cred = new HerokuCredentialsProvider(HerokuProperties.getString("heroku.eclipse.git.defaultUser"), ""); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// first clone

					App app = createHerokuApp(appName, template, new NullProgressMonitor());
					if (app != null) {
						// then materialize
						try {
							service.materializeGitApp(app, destinationDir, timeout,
									Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), cred, new NullProgressMonitor()); //$NON-NLS-1$
						}
						catch (HerokuServiceException e) {
							if (e.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
								namePage.setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists")); //$NON-NLS-1$
								namePage.setVisible(true);
							}
							else if (e.getErrorCode() == HerokuServiceException.INVALID_LOCAL_GIT_LOCATION) {
								HerokuUtils.userError(
										getShell(),
										Messages.getString("HerokuAppCreateNamePage_Error_GitLocationInvalid_Title"), Messages.getFormattedString("HerokuAppCreateNamePage_Error_GitLocationInvalid", destinationDir + System.getProperty("file.separator") + app.getName())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							}
							else {
								e.printStackTrace();
								Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
								HerokuUtils.internalError(getShell(), e);
							}
						}
					}
				}
			});
		}
		catch (InvocationTargetException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
			HerokuUtils.internalError(getShell(), e);
		}
		catch (InterruptedException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
			HerokuUtils.internalError(getShell(), e);
		}

		return rv;
	}

	/**
	 * Creates the app on the Heroku side
	 * 
	 * @return the newly created App instance
	 */
	private App createHerokuApp(String appName, AppTemplate template, IProgressMonitor pm) {
		App app = null;

		if (appName != null) {
			if (template != null) {
				try {
					app = service.createAppFromTemplate(appName, template.getTemplateName(), pm);
				}
				catch (HerokuServiceException e) {
					if (e.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
						getContainer().showPage(namePage);
						Activator.getDefault().getLogger()
								.log(LogService.LOG_WARNING, "Application '" + appName + "' already exists, denying creation", e); //$NON-NLS-1$ //$NON-NLS-2$
						namePage.setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists")); //$NON-NLS-1$
					}
					else {
						e.printStackTrace();
						Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
						HerokuUtils.herokuError(getShell(), e);
					}

				}
			}
		}

		return app;
	}
}
