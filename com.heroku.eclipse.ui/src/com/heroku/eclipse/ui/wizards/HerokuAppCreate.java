/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;

/**
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuAppCreate extends Wizard {
	private static final String STORE_SECTION = "HerokuAppCreateWizard"; //$NON-NLS-1$

	private HerokuAppCreateWizardPage page;
	
	/**
	 * 
	 */
	public HerokuAppCreate() {
		IDialogSettings masterSettings = getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setWindowTitle("here comes the window title");

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void addPages() {
		setNeedsProgressMonitor(false);

		try {
			page = new HerokuAppCreateWizardPage();
			addPage(page);
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Returns a section in the dialog settings. If the section doesn't exist yet, it is created.
	 *
	 * @param name the name of the section
	 * @return the section of the given name
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}
	
	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

}
