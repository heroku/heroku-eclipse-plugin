package com.heroku.eclipse.ui.wizards;

import javax.swing.ProgressMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;

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

		try {
			namePage = new HerokuAppCreateNamePage();
			addPage(namePage);
			templatePage = new HerokuAppCreateTemplatePage();
			addPage(templatePage);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performFinish() {
		boolean rv = false;
		
		NullProgressMonitor pm = new NullProgressMonitor();

		// first clone
		App app = createHerokuApp(pm);
		if (app != null) {
			// then materialize
			try {
				service.materializeGitApp(app, Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), pm); //$NON-NLS-1$
			}
			catch (HerokuServiceException e) {
				if ( e.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE ) {
					namePage.setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists")); //$NON-NLS-1$
					namePage.setVisible(true);
					return false;
				}
				else {
					e.printStackTrace();
				}
			}
			rv = true;
		}

		return rv;
	}

	/**
	 * Creates the app on the Heroku side
	 * 
	 * @return the newly created App instance
	 */
	private App createHerokuApp( IProgressMonitor pm ) {
		App app = null;

		String appName = namePage.getAppName();

		if (appName != null) {
			AppTemplate template = templatePage.getAppTemplate();

			if (template != null) {
				try {
					app = service.createAppFromTemplate(appName, template.getTemplateName(), pm);
				}
				catch (HerokuServiceException e) {
					e.printStackTrace();
				}
			}
		}

		return app;
	}
}
