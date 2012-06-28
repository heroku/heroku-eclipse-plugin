package com.heroku.eclipse.ui.preferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * The preferences page for the Heroclipse plugin
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	/**
	 * Preference page ID of our lovely page
	 */
	public static final String ID = "com.heroku.eclipse.ui.preferences.HerokuPreferencePage"; //$NON-NLS-1$

	private static final String HEROKU_PREFERENCE_PAGE_CONTEXT = "com.heroku.eclipse.context"; //$NON-NLS-1$

	private Map<String, Object> widgetRegistry = new HashMap<String, Object>();
	private Map<String, ControlDecoration> decoratorRegistry = new HashMap<String, ControlDecoration>();

	private HerokuServices service;

	@SuppressWarnings({ "deprecation", "restriction" })
	private org.eclipse.core.runtime.Preferences jschPreferences = JSchCorePlugin.getPlugin().getPluginPreferences();

	private Composite group;

	/**
	 * 
	 */
	public HerokuPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.getString("HerokuPreferencePage_Title")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}

	/*
	 * (non-Javadoc) -
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(final Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening Heroku preferences"); //$NON-NLS-1$

		group = new Composite(parent, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		group.setLayout(layout);

		// Email
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString("HerokuPreferencePage_Email")); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());
			t.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.P_EMAIL);
			t.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));

			ControlDecoration c = new ControlDecoration(t, SWT.BOTTOM | SWT.LEFT);
			c.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			c.setDescriptionText(Messages.getString("HerokuPreferencePage_Error_Decorator_EmailMissing")); //$NON-NLS-1$
			c.hide();

			widgetRegistry.put(PreferenceConstants.P_EMAIL, t);
			decoratorRegistry.put(PreferenceConstants.P_EMAIL, c);

			@SuppressWarnings("unused")
			Label dummy = new Label(group, SWT.NONE);
		}

		// Password
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString("HerokuPreferencePage_Password")); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

			Text t = new Text(group, SWT.PASSWORD | SWT.BORDER);
			t.setFont(group.getFont());
			t.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
			t.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.P_PASSWORD);

			ControlDecoration c = new ControlDecoration(t, SWT.BOTTOM | SWT.LEFT);
			c.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			c.setDescriptionText(Messages.getString("HerokuPreferencePage_Error_Decorator_PasswordMissing")); //$NON-NLS-1$
			c.hide();

			widgetRegistry.put(PreferenceConstants.P_PASSWORD, t);
			decoratorRegistry.put(PreferenceConstants.P_PASSWORD, c);

			Button b = new Button(group, SWT.PUSH);
			b.setText(Messages.getString("HerokuPreferencePage_GetAPIKey")); //$NON-NLS-1$
			b.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			b.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.B_FETCH_API_KEY);
			b.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					boolean isValid = true;
					setErrorMessage(null);

					// first validate input
					if (!validateLoginData(true)) {
						setErrorMessage(Messages.getString("HerokuPreferencePage_Error_PleaseCheckInput")); //$NON-NLS-1$
					}
					else {
						// then talk to Heroku
						String apiKey = retrieveAPIKey();

						// login failed
						if (apiKey == null) {
							isValid = false;
							setErrorMessage(Messages.getString("HerokuPreferencePage_Error_LoginFailed")); //$NON-NLS-1$

							try {
								((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).setText(""); //$NON-NLS-1$
								service.setAPIKey(null);
							}
							catch (HerokuServiceException e1) {
								isValid = false;
								HerokuUtils.herokuError(getShell(), e1);
							}

							Activator.getDefault().getLogger()
									.log(LogService.LOG_DEBUG, "login failed for user '" + widgetRegistry.get(PreferenceConstants.P_EMAIL) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						else {
							isValid = setAPIKey(apiKey);
							if (isValid) {
								((Button) widgetRegistry.get(PreferenceConstants.B_VALIDATE_API_KEY)).setEnabled(true);
								((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).setText(apiKey);
								setMessage(Messages.getString("HerokuPreferencePage_Info_Login_OK"), IMessageProvider.INFORMATION); //$NON-NLS-1$)
								Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "successfully logged into Heroku account"); //$NON-NLS-1$
							}
						}
					}

					((Button) widgetRegistry.get(PreferenceConstants.B_ADD_SSH_KEY)).setEnabled(isValid);
					((Button) widgetRegistry.get(PreferenceConstants.B_REMOVE_SSH_KEY)).setEnabled(isValid);

				}
			});
		}

		// API Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString("HerokuPreferencePage_APIKey")); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());
			t.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
			t.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.P_API_KEY);

			ControlDecoration c = new ControlDecoration(t, SWT.BOTTOM | SWT.LEFT);
			c.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			c.setDescriptionText(Messages.getString("HerokuPreferencePage_Error_Decorator_APIKeyMissing")); //$NON-NLS-1$
			c.hide();

			widgetRegistry.put(PreferenceConstants.P_API_KEY, t);
			decoratorRegistry.put(PreferenceConstants.P_API_KEY, c);

			Button b = new Button(group, SWT.PUSH);
			b.setText(Messages.getString("HerokuPreferencePage_Validate")); //$NON-NLS-1$
			b.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			b.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.B_VALIDATE_API_KEY);
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					boolean isValid = true;
					setErrorMessage(null);

					// first validate input
					if (!validateAPIKeyData(true)) {
						isValid = false;
						setErrorMessage(Messages.getString("HerokuPreferencePage_Error_PleaseCheckInput")); //$NON-NLS-1$
					}
					else {
						String apiKey = ((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).getText().trim();

						// then talk to Heroku
						String sshKey = ((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).getText();
						if (!sshKey.trim().isEmpty()) {
							isValid = setAPIAndSSHKey(apiKey, sshKey);
						}
						else {
							isValid = setAPIKey(apiKey);
						}

						if (isValid) {
							setMessage(Messages.getString("HerokuPreferencePage_Info_KeyValidation_OK"), IMessageProvider.INFORMATION); //$NON-NLS-1$)
							Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "validating API key: successfully listed all apps "); //$NON-NLS-1$
						}
					}

					((Button) widgetRegistry.get(PreferenceConstants.B_ADD_SSH_KEY)).setEnabled(isValid);
					((Button) widgetRegistry.get(PreferenceConstants.B_REMOVE_SSH_KEY)).setEnabled(isValid);
				}

			});

			widgetRegistry.put(PreferenceConstants.B_VALIDATE_API_KEY, b);
		}

		// SSH Key
		Group sshGroup = new Group(group, SWT.LEFT);
		GridLayout sshLayout = new GridLayout();
		sshGroup.setLayout(sshLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1);
		sshGroup.setLayoutData(data);
		sshGroup.setText(Messages.getString("HerokuPreferencePage_SSHKey")); //$NON-NLS-1$

		{
			Text t = new Text(sshGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
			t.setFont(sshGroup.getFont());
			GridData g = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			g.heightHint = 100;
			g.widthHint = 500;
			t.setLayoutData(g);
			t.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.P_SSH_KEY);

			widgetRegistry.put(PreferenceConstants.P_SSH_KEY, t);
		}

		// button row
		{
			Composite right = new Composite(sshGroup, SWT.NONE);
			right.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false, 3, 1));

			GridLayout gl = new GridLayout(4, true);
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			right.setLayout(gl);

			PreferenceLinkArea p = new PreferenceLinkArea(right, SWT.NONE,
					"org.eclipse.jsch.ui.SSHPreferences", Messages.getString("HerokuPreferencePage_Generate"),//$NON-NLS-1$ //$NON-NLS-2$
					(IWorkbenchPreferenceContainer) getContainer(), null);

			p.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

			Button load = new Button(right, SWT.PUSH);
			load.setText(Messages.getString("HerokuPreferencePage_LoadKey")); //$NON-NLS-1$
			load.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			load.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.B_LOAD_SSH_KEY);
			load.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					setErrorMessage(null);
					String pubKey = loadSSHPublicKeyFile();
					if (pubKey == null) {
						setErrorMessage(Messages.getString("HerokuPreferencePage_Error_SSHKeyInvalid")); //$NON-NLS-1$
					}
					else if (!pubKey.trim().isEmpty()) {
						setMessage(null);
						((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).setText(pubKey);
					}
				}

			});

			Button add = new Button(right, SWT.PUSH);
			add.setText(Messages.getString("HerokuPreferencePage_Add")); //$NON-NLS-1$
			add.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			add.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.B_ADD_SSH_KEY);
			add.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					setErrorMessage(null);
					String sshKey = ((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).getText();
					if (HerokuUtils.isNotEmpty(sshKey)) {
						if (setSSHKey(sshKey)) {
							setMessage(Messages.getString("HerokuPreferencePage_Info_SSHKeyAdd_OK"), IMessageProvider.INFORMATION); //$NON-NLS-1$)
						}
					}
				}
			});

			widgetRegistry.put(PreferenceConstants.B_ADD_SSH_KEY, add);

			Button remove = new Button(right, SWT.PUSH);
			remove.setText(Messages.getString("HerokuPreferencePage_Remove")); //$NON-NLS-1$
			remove.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			remove.setData(HerokuServices.ROOT_WIDGET_ID, PreferenceConstants.B_REMOVE_SSH_KEY);
			remove.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					setErrorMessage(null);
					String sshKey = ((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).getText().trim();
					if (!sshKey.isEmpty()) {
						if (removeSSHKey(sshKey)) {
							setMessage(Messages.getString("HerokuPreferencePage_Info_SSHKeyRemoval_OK"), IMessageProvider.INFORMATION); //$NON-NLS-1$)
						}
					}
				}

			});

			widgetRegistry.put(PreferenceConstants.B_REMOVE_SSH_KEY, remove);
		}

		initialize();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), HEROKU_PREFERENCE_PAGE_CONTEXT);

		applyDialogFont(group);

		return group;
	}

	/**
	 * Verifies that all data required for login is present
	 * 
	 * @param decorate
	 *            if set to true, the relevant fields will be decorated with an
	 *            error icon
	 * @return
	 */
	private boolean validateLoginData(boolean decorate) {
		boolean isValid = true;
		// validate Email/Password
		Text email = (Text) widgetRegistry.get(PreferenceConstants.P_EMAIL);
		Text password = (Text) widgetRegistry.get(PreferenceConstants.P_PASSWORD);

		if (email.getText().trim().isEmpty()) {
			if (decorate) {
				decoratorRegistry.get(PreferenceConstants.P_EMAIL).show();
			}
			isValid = false;
		}
		else {
			decoratorRegistry.get(PreferenceConstants.P_EMAIL).hide();
		}

		if (password.getText().trim().isEmpty()) {
			if (decorate) {
				decoratorRegistry.get(PreferenceConstants.P_PASSWORD).show();
			}
			isValid = false;
		}
		else {
			if (decorate) {
				decoratorRegistry.get(PreferenceConstants.P_PASSWORD).hide();
			}
		}
		return isValid;
	}

	/**
	 * Validates that all the data required for validating an API key is there
	 * 
	 * @param decorate
	 *            if set to true, the relevant fields will be decorated with an
	 *            error icon
	 * @return
	 */
	private boolean validateAPIKeyData(boolean decorate) {
		// validate API key
		boolean isValid = true;

		Text t = (Text) widgetRegistry.get(PreferenceConstants.P_API_KEY);

		if (t.getText().trim().isEmpty()) {
			if (decorate) {
				decoratorRegistry.get(PreferenceConstants.P_API_KEY).show();
			}
			isValid = false;
		}
		else {
			decoratorRegistry.get(PreferenceConstants.P_API_KEY).hide();
		}
		return isValid;
	}

	/**
	 * Retrieves the user's Heroku API key as a modal operation
	 * 
	 * @param service
	 */
	private String retrieveAPIKey() {
		final AtomicReference<String> apiKey = new AtomicReference<String>();

		final String email = ((Text) widgetRegistry.get(PreferenceConstants.P_EMAIL)).getText();
		final String password = ((Text) widgetRegistry.get(PreferenceConstants.P_PASSWORD)).getText();

		try {
			PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuPreferencePage_Progress_Login"), 2); //$NON-NLS-1$
					monitor.worked(1);
					try {
						apiKey.set(service.obtainAPIKey(email, password));
						monitor.worked(1);
						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (InvocationTargetException e1) {
			if (!(e1.getCause() instanceof HerokuServiceException)) {
				HerokuUtils.internalError(getShell(), e1);
			}
			else {
				HerokuUtils.herokuError(getShell(), e1);
			}
		}
		catch (InterruptedException e1) {
			HerokuUtils.internalError(getShell(), e1);
		}

		return apiKey.get();
	}

	/**
	 * Stores the user's Heroku API key as a modal operation
	 * 
	 * @param apiKey
	 */
	private boolean setAPIKey(final String apiKey) {
		boolean rv = false;
		try {
			PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuPreferencePage_Progress_ValidateApiKey"), 2); //$NON-NLS-1$
					monitor.worked(1);
					try {
						service.setAPIKey(apiKey);
						monitor.worked(1);
						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});

			return true;
		}
		catch (InvocationTargetException e1) {
			if ((e1.getCause() instanceof HerokuServiceException)) {
				if (((HerokuServiceException) e1.getCause()).getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_SecureStoreUnvailable")); //$NON-NLS-1$
				}
				else {
					HerokuUtils.herokuError(getShell(), e1);
				}
			}
			else {
				HerokuUtils.internalError(getShell(), e1);
			}
		}
		catch (InterruptedException e1) {
			HerokuUtils.internalError(getShell(), e1);
		}

		return rv;
	}

	/**
	 * Stores the user's Heroku API and SSH keys as a modal operation
	 * 
	 * @param apiKey
	 */
	private boolean setAPIAndSSHKey(final String apiKey, final String sshKey) {
		boolean rv = false;
		try {
			PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuPreferencePage_Progress_ValidateApiKey"), 3); //$NON-NLS-1$
					monitor.worked(1);
					try {

						service.setAPIKey(apiKey);
						monitor.worked(1);

						service.setSSHKey(sshKey);
						monitor.worked(1);

						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});

			return true;
		}
		catch (InvocationTargetException e1) {
			if ((e1.getCause() instanceof HerokuServiceException)) {
				HerokuServiceException e2 = (HerokuServiceException) e1.getCause();
				if (e2.getErrorCode() == HerokuServiceException.INVALID_API_KEY) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_KeyValidationFailed")); //$NON-NLS-1$
				}
				else if (e2.getErrorCode() == HerokuServiceException.INVALID_SSH_KEY || e2.getErrorCode() == HerokuServiceException.SSH_KEY_ALREADY_EXISTS) {
					// don't disable buttons at this stage
					rv = true;
				}
				else if (e2.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_SecureStoreUnvailable")); //$NON-NLS-1$
				}
				else {
					HerokuUtils.herokuError(getShell(), e2);
				}
			}
			else {
				HerokuUtils.internalError(getShell(), e1);
			}
		}
		catch (InterruptedException e1) {
			HerokuUtils.internalError(getShell(), e1);
		}

		return rv;
	}

	/**
	 * Stores the user's SSH API key as a modal operation
	 * 
	 * @param sshKey
	 */
	private boolean setSSHKey(final String sshKey) {
		boolean rv = false;
		try {
			PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuPreferencePage_Progress_StoringSSHKey"), 2); //$NON-NLS-1$
					monitor.worked(1);
					try {
						service.setSSHKey(sshKey);
						monitor.worked(1);
						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});

			return true;
		}
		catch (InvocationTargetException e1) {
			if ((e1.getCause() instanceof HerokuServiceException)) {
				HerokuServiceException e2 = (HerokuServiceException) e1.getCause();
				if (e2.getErrorCode() == HerokuServiceException.INVALID_API_KEY) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_KeyValidationFailed")); //$NON-NLS-1$
				}
				else if (e2.getErrorCode() == HerokuServiceException.INVALID_SSH_KEY || e2.getErrorCode() == HerokuServiceException.SSH_KEY_ALREADY_EXISTS) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_SSHKeyInvalid")); //$NON-NLS-1$
				}
				else {
					HerokuUtils.herokuError(getShell(), e2);
				}
			}
			else {
				HerokuUtils.internalError(getShell(), e1);
			}
		}
		catch (InterruptedException e1) {
			HerokuUtils.internalError(getShell(), e1);
		}

		return rv;
	}

	/**
	 * Removes the user's SSH API key as a modal operation
	 * 
	 * @param sshKey
	 */
	private boolean removeSSHKey(final String sshKey) {
		boolean rv = false;
		try {
			PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuPreferencePage_Progress_RemovingSSHKey"), 2); //$NON-NLS-1$
					monitor.worked(1);
					try {
						service.removeSSHKey(sshKey);
						monitor.worked(1);
						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});

			return true;
		}
		catch (InvocationTargetException e1) {
			if ((e1.getCause() instanceof HerokuServiceException)) {
				HerokuServiceException e2 = (HerokuServiceException) e1.getCause();

				if (e2.getErrorCode() == HerokuServiceException.NOT_FOUND) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_UnknownSSHKey")); //$NON-NLS-1$
				}
				else if (e2.getErrorCode() == HerokuServiceException.INVALID_SSH_KEY) {
					setErrorMessage(Messages.getString("HerokuPreferencePage_Error_SSHKeyInvalid")); //$NON-NLS-1$
				}
				else {
					HerokuUtils.herokuError(getShell(), e2);
				}
			}
			else {
				HerokuUtils.internalError(getShell(), e1);
			}
		}
		catch (InterruptedException e1) {
			HerokuUtils.internalError(getShell(), e1);
		}

		return rv;
	}

	/**
	 * Load and validates a ssh DSA or RSA public key as found in a user
	 * specified file
	 * 
	 * @return the public key or null if anything went wrong
	 */
	private String loadSSHPublicKeyFile() {
		String publicKey = ""; //$NON-NLS-1$

		@SuppressWarnings({ "restriction", "deprecation" })
		String sshHome = jschPreferences.getString(org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME);

		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setFilterPath(sshHome);
		fd.setFilterExtensions(new String[] { "*.pub" }); //$NON-NLS-1$

		Object o = fd.open();

		if (o != null) {
			publicKey = readSSHKey(new File(fd.getFilterPath(), fd.getFileName()));
		}

		return publicKey;
	}

	/**
	 * @param keyFile
	 * @return
	 */
	private String readSSHKey(File keyFile) {
		String publicKey = ""; //$NON-NLS-1$

		if (keyFile.exists() && keyFile.length() <= 1024) {
			byte[] buffer = new byte[(int) keyFile.length()];
			BufferedInputStream f;

			try {
				f = new BufferedInputStream(new FileInputStream(keyFile));
				f.read(buffer);
				publicKey = new String(buffer);

				service.validateSSHKey(publicKey);
			}
			catch (FileNotFoundException e) {
				publicKey = null;
				HerokuUtils.internalError(getShell(), e);
			}
			catch (IOException e) {
				publicKey = null;
				HerokuUtils.internalError(getShell(), e);
			}
			catch (HerokuServiceException e) {
				if (e.getErrorCode() == HerokuServiceException.INVALID_SSH_KEY) {
					publicKey = null;
				}
				else {
					HerokuUtils.herokuError(getShell(), e);
				}
			}
		}
		return publicKey;
	}

	/**
	 * Initializes the page and loads existing preferences
	 * 
	 * @throws BackingStoreException
	 */
	private void initialize() {
		try {
			setErrorMessage(null);
			group.setEnabled(true);

			String apiKey = service.getAPIKey();

			((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).setText(HerokuUtils.ensureNotNull(apiKey));

			// primary source for the SSH key are the preferences
			String sshKey = service.getSSHKey();

			// if the prefs are empty, ask eclipse
			if (sshKey == null || sshKey.isEmpty()) {
				@SuppressWarnings({ "restriction", "deprecation" })
				String sshHome = jschPreferences.getString(org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME);

				if (HerokuUtils.isNotEmpty(sshHome)) {
					File keyFile = null;

					// if we have key preferences, use them
					@SuppressWarnings({ "restriction", "deprecation" })
					String wantedKeys = jschPreferences.getString(org.eclipse.jsch.internal.core.IConstants.KEY_PRIVATEKEY);

					if (HerokuUtils.isNotEmpty(wantedKeys)) {
						// we use the first found .pub key file, controlled by
						// the user's precedence
						String[] keyfiles = wantedKeys.split(","); //$NON-NLS-1$
						for (int i = 0; i < keyfiles.length; i++) {
							keyfiles[i] += ".pub"; //$NON-NLS-1$

							keyFile = new File(keyfiles[i]);
							if (!keyFile.isAbsolute()) {
								keyFile = new File(sshHome, keyfiles[i]);
							}
							if (keyFile.exists()) {
								break;
							}
							else {
								keyFile = null;
							}
						}
					}
					else {
						// otherwise, if there is only one .pub, use that one
						File sshDir = new File(sshHome);
						String[] pubkeyFiles = sshDir.list(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.endsWith(".pub"); //$NON-NLS-1$
							}
						});

						if (pubkeyFiles != null && pubkeyFiles.length == 1) {
							keyFile = new File(sshHome, pubkeyFiles[0]);
						}
					}

					if (keyFile != null) {
						sshKey = readSSHKey(keyFile);
					}
				}
			}

			((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).setText(HerokuUtils.ensureNotNull(sshKey));

			boolean existingAPIKey = validateAPIKeyData(false);

			((Button) widgetRegistry.get(PreferenceConstants.B_VALIDATE_API_KEY)).setEnabled(existingAPIKey);
			((Button) widgetRegistry.get(PreferenceConstants.B_ADD_SSH_KEY)).setEnabled(existingAPIKey);
			((Button) widgetRegistry.get(PreferenceConstants.B_REMOVE_SSH_KEY)).setEnabled(existingAPIKey);
		}
		catch (HerokuServiceException e1) {
			if (e1.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("Heroku_Common_Error_SecureStoreInvalid"), e1); //$NON-NLS-1$

				ErrorDialog.openError(getShell(), Messages.getString("Heroku_Common_Error_SecureStoreInvalid_Title"), null, status); //$NON-NLS-1$

				setErrorMessage(Messages.getString("Heroku_Common_Error_SecureStoreInvalid")); //$NON-NLS-1$

				group.setEnabled(false);

				return;
			}
			else {
				HerokuUtils.internalError(getShell(), e1);
			}
		}

		if (System.getProperty("heroku.devel") != null && System.getProperty("heroku.devel").equals("true")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String develUser = System.getProperty("heroku.junit.user1") == null ? System.getenv("HEROKU_TEST_USERNAME_1") : System.getProperty("heroku.junit.user1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (develUser != null) {
				((Text) widgetRegistry.get(PreferenceConstants.P_EMAIL)).setText(develUser);
			}

			String develPwd = System.getProperty("heroku.junit.pwd1") == null ? System.getenv("HEROKU_TEST_PWD_1") : System.getProperty("heroku.junit.pwd1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (develPwd != null) {
				((Text) widgetRegistry.get(PreferenceConstants.P_PASSWORD)).setText(develPwd);
			}
		}
	}

	@Override
	public boolean performOk() {
		performApply();
		return super.performOk();
	}

	@Override
	protected void performApply() {
		setAPIAndSSHKey(((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).getText().trim(), ((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY))
				.getText().trim());
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();

		try {
			service.setAPIKey(null);
			service.setSSHKey(null);

			initialize();
		}
		catch (HerokuServiceException e) {
			HerokuUtils.herokuError(getShell(), e);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			initialize();
		}
	}

}
