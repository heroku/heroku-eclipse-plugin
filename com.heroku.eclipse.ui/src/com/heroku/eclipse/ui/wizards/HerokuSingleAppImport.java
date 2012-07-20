package com.heroku.eclipse.ui.wizards;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices.IMPORT_TYPES;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * Import wizard for one, predefined existing Heroku App
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuSingleAppImport extends AbstractHerokuAppImportWizard implements IImportWizard {
	private HerokuAppProjectTypePage projectTypePage;
	private App app;

	/**
	 * @param app 
	 */
	public HerokuSingleAppImport( App app) {
		super();
		this.app = app;
	}

	@Override
	public void addPages() {
		setNeedsProgressMonitor(false);

		if (HerokuUtils.verifyPreferences(new NullProgressMonitor(), service, Display.getCurrent().getActiveShell())) {
			try {
				projectTypePage = new HerokuAppProjectTypePage();
				projectTypePage.setTitle(Messages.getString("HerokuAppProjectType_SingleTitle")); //$NON-NLS-1$
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
	public App getActiveApp() {
		return app;
	}
	
	@Override
	public IMPORT_TYPES getProjectType() {
		return projectTypePage.getImportType();
	}
}
