/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuServices.IMPORT_TYPES;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * Wizard page allowing the user to choose how a project is imported
 *   
 * @author udo.rader@bestsolution.at
 */
public class HerokuAppProjectTypePage extends WizardPage {
	private HerokuServices service;
	private Button autodetect;
	private Button newProject;
	private Button generalProject;

	private IMPORT_TYPES importType = IMPORT_TYPES.AUTODETECT;

	/**
	 * constructor 
	 */
	public HerokuAppProjectTypePage() {
		super("HerokuAppProjectTypePage"); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppProjectType_Title")); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppProjectType_Description")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}

	@Override
	public void createControl(Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening app import project type wizard page"); //$NON-NLS-1$

		Group group = new Group(parent, SWT.SHADOW_IN);
		group.setEnabled(true);
		setControl(group);

		setErrorMessage(null);
		setPageComplete(true);

		group.setText(Messages.getString("HerokuAppProjectType_WizardTitle")); //$NON-NLS-1$
		group.setLayout(new RowLayout(SWT.VERTICAL));
		
		autodetect = new Button(group, SWT.RADIO);
		autodetect.setText(Messages.getString("HerokuAppProjectType_Import_Autodetect")); //$NON-NLS-1$
		autodetect.addSelectionListener(getSelectionListener());

		newProject = new Button(group, SWT.RADIO);
		newProject.setText(Messages.getString("HerokuAppProjectType_Import_NewProjectWizard")); //$NON-NLS-1$
		newProject.addSelectionListener(getSelectionListener());

		generalProject = new Button(group, SWT.RADIO);
		generalProject.setText(Messages.getString("HerokuAppProjectType_Import_GeneralProject")); //$NON-NLS-1$
		generalProject.addSelectionListener(getSelectionListener());
		
	}

	private SelectionListener getSelectionListener() {
		SelectionListener s = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (autodetect.getSelection()) {
					importType = IMPORT_TYPES.AUTODETECT;
				}
				else if (newProject.getSelection()) {
					importType = IMPORT_TYPES.WIZARD;
				}
				else if (generalProject.getSelection()) {
					importType = IMPORT_TYPES.GENERAL_PROJECT;
				}
			}
		};
		return s;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			String detectedType = Messages.getString("HerokuAppProjectType_Import_Autodetect"); //$NON-NLS-1$
			App activeApp = ((HerokuAppImport) getWizard()).getActiveApp();
			if ( activeApp != null && HerokuUtils.isNotEmpty(activeApp.getBuildpackProvidedDescription())) {
				IMPORT_TYPES type = service.getProjectType(activeApp.getBuildpackProvidedDescription());
				
				switch ( type ) {
					case MAVEN:
						detectedType = Messages.getFormattedString("HerokuAppProjectType_Import_AutodetectWithType", Messages.getString("HerokuAppProjectType_Import_Type_Maven") ); //$NON-NLS-1$ //$NON-NLS-2$
						break;
					case PLAY:
						detectedType = Messages.getFormattedString("HerokuAppProjectType_Import_AutodetectWithType", Messages.getString("HerokuAppProjectType_Import_Type_Play") ); //$NON-NLS-1$ //$NON-NLS-2$
						break;
				}
			}
			
			autodetect.setFocus();
			autodetect.setSelection(true);
			autodetect.setText(detectedType);
			
			autodetect.getParent().layout(true);
		}
	}

	/**
	 * @return
	 */
	public IMPORT_TYPES getImportType() {
		return importType;
	}

}
