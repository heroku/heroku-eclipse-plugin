package com.heroku.eclipse.ui.wizards;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Import wizard for existing Heroku apps
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuAppImport extends Wizard implements IImportWizard {
	private HerokuAppImportWizardPage page;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(false);

		try {
			page = new HerokuAppImportWizardPage();
			addPage(page);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performFinish() {
		MessageDialog.openInformation(getShell(), "hmm", "who cares about the spanish inquisition?");
		return true;
	}

}
