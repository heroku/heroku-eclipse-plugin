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
import com.heroku.eclipse.core.services.HerokuServices.IMPORT_TYPES;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.git.HerokuCredentialsProvider;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * Wizard allowing to create a new Heroku App
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppCreate extends Wizard implements IImportWizard {

	private HerokuAppCreatePage createPage;

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

		if (HerokuUtils.verifyPreferences(new NullProgressMonitor(), service, Display.getCurrent().getActiveShell())) {
			try {
				createPage = new HerokuAppCreatePage();
				addPage(createPage);
			}
			catch (Exception e) {
				HerokuUtils.internalError(Display.getCurrent().getActiveShell(), e);
			}
		}
		else {
			// TODO: closes the entire Eclipse when the failure was due to the secure store
			Display.getDefault().getActiveShell().close();
		}
	}

	@Override
	public boolean performFinish() {
		final String appName = createPage.getAppName().toLowerCase();

		try {
			// ensure that the name is available
			if (HerokuUtils.isNotEmpty(appName) && (!service.isAppNameBasicallyValid(appName) || service.appNameExists(new NullProgressMonitor(), appName))) {
				createPage.displayInvalidNameWarning();
				return false;
			}

			final AppTemplate template = createPage.getAppTemplate();

			final String destinationDir = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR)+
					System.getProperty("file.separator")+HerokuProperties.getString("defaultRepo"); //$NON-NLS-1$ //$NON-NLS-2$
			final int timeout = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			final HerokuCredentialsProvider cred = new HerokuCredentialsProvider(HerokuProperties.getString("heroku.eclipse.git.defaultUser"), ""); //$NON-NLS-1$ //$NON-NLS-2$

			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						if ( HerokuUtils.isNotEmpty(appName)) {
							monitor.beginTask(Messages.getFormattedString("HerokuAppCreate_CreatingApp", appName), 2); //$NON-NLS-1$
						}
						else {
							monitor.beginTask(Messages.getString("HerokuAppCreate_CreatingArbitraryApp"), 2); //$NON-NLS-1$
						}
						monitor.subTask(Messages.getString("HerokuAppCreate_CloningTemplate")); //$NON-NLS-1$
						monitor.worked(1);
						// then materialize
						try {
							// first clone
							App app = service.createAppFromTemplate(monitor, appName, template.getTemplateName());
						
							if (app != null) {
								monitor.subTask(Messages.getString("HerokuAppCreate_FetchingApp")); //$NON-NLS-1$
								service.materializeGitApp(monitor, app, IMPORT_TYPES.AUTODETECT, null, destinationDir, timeout,
										Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), cred); //$NON-NLS-1$
								monitor.worked(1);
								monitor.done();
							}
						}
						catch (HerokuServiceException e) {
							throw new InvocationTargetException(e);
						}
					}
				});
			}
			catch (InvocationTargetException e) {
				if (e.getCause() instanceof HerokuServiceException) {
					HerokuServiceException e1 = (HerokuServiceException) e.getCause();

					if (e1.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
						Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "Application '" + appName + "' already exists, denying creation", e); //$NON-NLS-1$ //$NON-NLS-2$
						createPage.displayInvalidNameWarning();
					}
					else if (e1.getErrorCode() == HerokuServiceException.INVALID_LOCAL_GIT_LOCATION) {
						HerokuUtils
								.userError(
										getShell(),
										Messages.getString("HerokuAppCreateNamePage_Error_GitLocationInvalid_Title"), Messages.getFormattedString("HerokuAppCreateNamePage_Error_GitLocationInvalid", destinationDir + System.getProperty("file.separator") + appName)); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					}
					else if ( e1.getErrorCode() != HerokuServiceException.OPERATION_CANCELLED ) {
						Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
						HerokuUtils.herokuError(getShell(), e);
					}
				}
				else {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
					HerokuUtils.internalError(getShell(), e);
				}
				return false;
			}
			catch (InterruptedException e) {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e); //$NON-NLS-1$
				HerokuUtils.internalError(getShell(), e);
				return false;
			}
		}
		catch (HerokuServiceException e1) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error, aborting ...", e1); //$NON-NLS-1$
			HerokuUtils.herokuError(getShell(), e1);
			return false;
		}

		return true;
	}
}
