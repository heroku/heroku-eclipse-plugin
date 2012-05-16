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
	
	/**
	 * @param pageName
	 */
	protected HerokuAppCreateNamePage(String pageName) {
		super(pageName);
	}

	/**
	 * 
	 */
	public HerokuAppCreateNamePage() {
		super("HerokuAppCreateNamePage"); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppCreateNamePage_Description")); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppCreateNamePage_Title")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
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
			
			final Text t = new Text(group, SWT.BORDER);
			t.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 1, 1 ) );
			t.setTextLimit( 100 );
			t.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					setErrorMessage(null);
					setPageComplete(true);
					if ( t.getText() == null || t.getText().trim().isEmpty() ) {
						setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameEmpty")); //$NON-NLS-1$
						setPageComplete(false);
					}
				}
			});
		}

	}

	/**
	 * Ensures that the preferences are setup
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
			else if (e.getErrorCode() == HerokuServiceException.INVALID_PREFERENCES) {
				HerokuUtils
						.userError(
								parent.getShell(),
								Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Title"), Messages.getString("Heroku_Common_Error_HerokuPrefsMissing")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			else {
				e.printStackTrace();
				HerokuUtils.internalError(parent.getShell(), e);
				return false;
			}
		}

		return isOk;
	}
}
