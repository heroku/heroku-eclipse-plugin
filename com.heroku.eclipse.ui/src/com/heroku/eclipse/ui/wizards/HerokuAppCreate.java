/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuAppCreate extends Wizard implements IImportWizard {
	
	private HerokuAppCreateNamePage namePage;
	private HerokuAppCreateTemplatePage templatePage;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
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
		
		MessageDialog.openInformation(getShell(), "hmm", "the larch");
		return true;
	}
}
