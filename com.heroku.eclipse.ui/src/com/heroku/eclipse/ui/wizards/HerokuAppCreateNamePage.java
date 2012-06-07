/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.log.LogService;

import com.heroku.eclipse.core.constants.AppCreateConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppCreateNamePage extends WizardPage {
	private HerokuServices service;
	private Text tAppName;

	/**
	 * 
	 */
	public HerokuAppCreateNamePage() {
		super("HerokuAppCreateNamePage"); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppCreateNamePage_Description")); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppCreateNamePage_Title")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening app create wizard, app name page"); //$NON-NLS-1$

		Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		setControl(group);

		group.setEnabled(true);
		setErrorMessage(null);
		setPageComplete(false);

		// ensure valid prefs
		if (!verifyPreferences(group)) {
			Activator.getDefault().getLogger().log(LogService.LOG_INFO, "preferences are missing/invalid"); //$NON-NLS-1$
			group.setEnabled(false);
			setErrorMessage(Messages.getString("Heroku_Common_Error_HerokuPrefsMissing")); //$NON-NLS-1$
		}
		else {
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString("HerokuAppCreateNamePage_Name")); //$NON-NLS-1$

			tAppName = new Text(group, SWT.BORDER);
			tAppName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
			tAppName.setTextLimit(100);
			tAppName.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.C_APP_NAME);
			tAppName.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					setErrorMessage(null);
					setPageComplete(true);
					if (!HerokuUtils.isNotEmpty(tAppName.getText())) {
						setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameEmpty")); //$NON-NLS-1$
						setPageComplete(false);
					}
				}
			});
		}

	}

	/**
	 * Ensures that the preferences are setup
	 * 
	 * @param parent
	 * @return true, if the prefs are OK, false if not
	 */
	private boolean verifyPreferences(Composite parent) {
		boolean isOk = true;

		try {
			isOk = service.isReady();
		}
		catch (HerokuServiceException e) {
			if (e.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
				HerokuUtils.userError(parent.getShell(),
						Messages.getString("Heroku_Common_Error_SecureStoreInvalid_Title"), Messages.getString("Heroku_Common_Error_SecureStoreInvalid")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			else {
				e.printStackTrace();
				HerokuUtils.internalError(parent.getShell(), e);
				return false;
			}
		}

		if (!isOk) {
			HerokuUtils.userError(parent.getShell(),
					Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Title"), Messages.getString("Heroku_Common_Error_HerokuPrefsMissing")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return isOk;
	}

	/**
	 * The name of the App to create
	 * 
	 * @return the name of the App to create
	 */
	public String getAppName() {
		return tAppName.getText();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			tAppName.setFocus();
		}
	}
}
