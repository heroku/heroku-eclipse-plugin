package com.heroku.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
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
 * Import wizard for existing Heroku apps
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppImport extends Wizard implements IImportWizard {
	private HerokuAppImportWizardPage listPage;

	private HerokuServices service;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
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

		try {
			listPage = new HerokuAppImportWizardPage();
			addPage(listPage);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performFinish() {
		final App app = listPage.getSelectedApp();

		if (app != null) {
			final String destinationDir = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
			final int timeout = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			final HerokuCredentialsProvider cred = new HerokuCredentialsProvider(HerokuProperties.getString("heroku.eclipse.git.defaultUser"), ""); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							service.materializeGitApp(app, destinationDir, timeout,
									Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), cred, new NullProgressMonitor()); //$NON-NLS-1$
						}
						catch (HerokuServiceException e) {
							if ( e.getErrorCode() == HerokuServiceException.INVALID_LOCAL_GIT_LOCATION ) {
								HerokuUtils.userError(getShell(), Messages.getString("HerokuAppCreateNamePage_Error_GitLocationInvalid_Title"), Messages.getFormattedString("replacements)HerokuAppCreateNamePage_Error_GitLocationInvalid", destinationDir+System.getProperty("path.separator")+app.getName()));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							}
							else {
								e.printStackTrace();
								Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
								HerokuUtils.internalError(getShell(), e);
							}
						}
					}
				});
			}
			catch (InvocationTargetException e) {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
				HerokuUtils.internalError(getShell(), e);
			}
			catch (InterruptedException e) {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
				HerokuUtils.internalError(getShell(), e);
			}
		}

		return true;
	}

}
