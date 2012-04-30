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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;

/**
 * The preferences page for the Heroclipse plugin
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String HEROKU_PREFERENCE_PAGE_CONTEXT = "com.heroku.eclipse.context"; //$NON-NLS-1$

	private Map<String, Object> widgetRegistry = new HashMap<String, Object>();
	private Map<String, ControlDecoration> decoratorRegistry = new HashMap<String, ControlDecoration>();

	private HerokuServices service;
	
	private Composite parent;

	@SuppressWarnings({ "deprecation", "restriction" })
	private org.eclipse.core.runtime.Preferences jschPreferences = JSchCorePlugin.getPlugin().getPluginPreferences();

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
		this.parent = parent;
		
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening Heroku preferences"); //$NON-NLS-1$

		final Composite group = new Composite(parent, SWT.NULL);

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

			ControlDecoration c = new ControlDecoration(t, SWT.BOTTOM | SWT.LEFT);
			c.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			c.setDescriptionText(Messages.getString("HerokuPreferencePage_Error_Decorator_PasswordMissing")); //$NON-NLS-1$
			c.hide();

			widgetRegistry.put(PreferenceConstants.P_PASSWORD, t);
			decoratorRegistry.put(PreferenceConstants.P_PASSWORD, c);

			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.getString("HerokuPreferencePage_GetAPIKey")); //$NON-NLS-1$
			b.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			b.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
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
							setErrorMessage(Messages.getString("HerokuPreferencePage_Error_LoginFailed")); //$NON-NLS-1$

							Activator.getDefault().getLogger()
									.log(LogService.LOG_DEBUG, "login failed for user '" + widgetRegistry.get(PreferenceConstants.P_EMAIL) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						else {
							try {
								service.setAPIKey(apiKey);

								((Button) widgetRegistry.get(PreferenceConstants.P_VALIDATE_API_KEY)).setEnabled(true);
								((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).setText(apiKey);
								
								setMessage(Messages.getString("HerokuPreferencePage_Info_Login_OK"), IMessageProvider.INFORMATION); //$NON-NLS-1$)
								Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "successfully logged into Heroku account"); //$NON-NLS-1$
							}
							catch (HerokuServiceException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
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

			ControlDecoration c = new ControlDecoration(t, SWT.BOTTOM | SWT.LEFT);
			c.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			c.setDescriptionText(Messages.getString("HerokuPreferencePage_Error_Decorator_APIKeyMissing")); //$NON-NLS-1$
			c.hide();

			widgetRegistry.put(PreferenceConstants.P_API_KEY, t);
			decoratorRegistry.put(PreferenceConstants.P_API_KEY, c);

			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.getString("HerokuPreferencePage_Validate")); //$NON-NLS-1$
			b.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setErrorMessage(null);

					// first validate input
					if (!validateAPIKeyData(true)) {
						setErrorMessage(Messages.getString("HerokuPreferencePage_Error_PleaseCheckInput")); //$NON-NLS-1$
					}
					else {
						// then talk to Heroku
						String apiKey = ((Text)widgetRegistry.get(PreferenceConstants.P_API_KEY)).getText().trim();
						
						try {
							service.validateAPIKey( apiKey );
							
							setMessage(Messages.getString("HerokuPreferencePage_Info_KeyValidation_OK"), IMessageProvider.INFORMATION); //$NON-NLS-1$)
							Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "validating API key: successfully listed all apps "); //$NON-NLS-1$
							
//							// immediately store a possibly existing SSH key
//							// (explicit request to do so w/o waiting for
//							// "apply")
							storeSSHKey();

						}
						catch (HerokuServiceException e1) {
							setErrorMessage(Messages.getString("HerokuPreferencePage_Error_KeyValidationFailed")); //$NON-NLS-1$
						}
					}
				}

			});

			widgetRegistry.put(PreferenceConstants.P_VALIDATE_API_KEY, b);
		}

		// SSH Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString("HerokuPreferencePage_SSHKey")); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));

			Text t = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY );
			t.setFont(group.getFont());
			GridData g = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
			g.heightHint = 100;
			g.widthHint = 300;
			t.setLayoutData(g);

			widgetRegistry.put(PreferenceConstants.P_SSH_KEY, t);

		}

		// button row
		{
			Composite right = new Composite(group, SWT.NONE);
			right.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false, 3, 1));

			GridLayout gl = new GridLayout(4, true);
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			right.setLayout(gl);

			PreferenceLinkArea p = new PreferenceLinkArea(right, SWT.NONE,
					"org.eclipse.jsch.ui.SSHPreferences", Messages.getString("HerokuPreferencePage_Generate"),//$NON-NLS-1$ //$NON-NLS-2$
					(IWorkbenchPreferenceContainer) getContainer(), null);

			p.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

			Button load = new Button(right, SWT.NULL);
			load.setText(Messages.getString("HerokuPreferencePage_LoadKey")); //$NON-NLS-1$
			load.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			load.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					String pubKey = loadSSHPublicKey();
					if ( pubKey == null ) {
						setErrorMessage(Messages.getString("HerokuPreferencePage_Error_SSHKeyInvalid")); //$NON-NLS-1$
					}
					else {
						((Text)widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).setText( pubKey );
					}
				}

			});

			Button upd = new Button(right, SWT.NULL);
			upd.setText(Messages.getString("HerokuPreferencePage_Update")); //$NON-NLS-1$
			upd.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			upd.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					storeSSHKey();
				}

			});

			Button clr = new Button(right, SWT.NULL);
			clr.setText(Messages.getString("HerokuPreferencePage_Clear")); //$NON-NLS-1$
			clr.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			clr.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					String sshKey = ((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).getText().trim();
					if (!sshKey.isEmpty()) {
						try {
							service.getOrCreateHerokuSession().removeSSHKey(sshKey);
						}
						catch (HerokuServiceException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}

			});
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
	 * Retrieves the user's Heroku API key
	 * 
	 * @param service
	 */
	private String retrieveAPIKey() {
		final AtomicReference<String> apiKey = new AtomicReference<String>();

		final String email = ((Text) widgetRegistry.get(PreferenceConstants.P_EMAIL)).getText();
		final String password = ((Text) widgetRegistry.get(PreferenceConstants.P_PASSWORD)).getText();

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						apiKey.set(service.obtainAPIKey(email, password));
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (InvocationTargetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return apiKey.get();
	}

	/**
	 * Load and validates a ssh DSA or RSA public key as found in a user specified file
	 * @return the public key or null if anything went wrong
	 */
	private String loadSSHPublicKey() {
		String publicKey = "";

		@SuppressWarnings({ "restriction", "deprecation" })
		String sshHome = jschPreferences.getString(org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME);

		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setFilterPath(sshHome);
		fd.setFilterExtensions(new String[]{"*.pub"}); //$NON-NLS-1$

		Object o = fd.open();

		if (o != null) { 
			String filename = fd.getFileName();
			File keyFile = new File(fd.getFilterPath(), filename);
			
			if ( keyFile.length() <= 1024 ) {
				byte[] buffer = new byte[(int) keyFile.length()];
				BufferedInputStream f;

				try {
					f = new BufferedInputStream(new FileInputStream(keyFile));
					f.read(buffer);
					publicKey = new String(buffer);
				}
				catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					publicKey = null;
					e.printStackTrace();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					publicKey = null;
					e.printStackTrace();
				}
			}
		}
		
		return publicKey;
	}

	/**
	 * If present, stores the SSH key both in the user's Heroku account and in
	 * the preferences
	 * 
	 * @param service
	 */
	private void storeSSHKey() {
		String sshKey = ((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).getText().trim();

		if (!sshKey.isEmpty()) {
			try {
				service.getOrCreateHerokuSession().addSSHKey(sshKey);
				service.setSSHKey(sshKey);
			}
			catch (HerokuServiceException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Initializes the page and loads existing preferences
	 * 
	 * @throws BackingStoreException
	 */
	private void initialize() {
		((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).setText(ensureNotNull( service.getAPIKey()));

		// ssh key:
		// +  * add "load public key" button
		// +  * if in prefs => DISPLAY in r/o text ara
		// +  * else if ssh-home found
		// +  ** if only one *pub => load & display immediately in r/o text area
		//   ** if more than one *pub => nada
		//   * else disable update & clear
		
		// primary source for the SSH key are the preferences 
		String sshKey = service.getSSHKey();
		
		// if the prefs are empty, ask eclipse
		if ( sshKey == null || sshKey.isEmpty() ) {
			@SuppressWarnings({ "restriction", "deprecation" })
			String sshHome = jschPreferences.getString(org.eclipse.jsch.internal.core.IConstants.KEY_SSH2HOME);

			File sshDir = new File(sshHome);
			String[] pubkeyFiles = sshDir.list( new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".pub"); //$NON-NLS-1$
				}
			});
			
			// if we find exactly one .pub file, load and dissplay it immediately for usage
			if (pubkeyFiles != null && pubkeyFiles.length == 1 ) {
		        String filename = pubkeyFiles[0];
		        
				File keyFile = new File(sshHome, filename);
				
				if ( keyFile.length() <= 1024 ) {
					byte[] buffer = new byte[(int) keyFile.length()];
					BufferedInputStream f;

					try {
						f = new BufferedInputStream(new FileInputStream(keyFile));
						f.read(buffer);
						sshKey = new String(buffer);
					}
					catch (FileNotFoundException e) {
//						ErrorDialog.openError(parent, "", "", IStatus.ERROR);
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).setText(ensureNotNull(sshKey));

		((Button) widgetRegistry.get(PreferenceConstants.P_VALIDATE_API_KEY)).setEnabled(validateAPIKeyData(false));
	}
	
	
	private static String ensureNotNull( String nullable ) {
		return ( nullable == null ) ? "" : nullable; //$NON-NLS-1$
	}
	
	@Override
	public boolean performOk() {
		performApply();
		return super.performOk();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply() {
		try {
			service.setAPIKey(((Text) widgetRegistry.get(PreferenceConstants.P_API_KEY)).getText().trim());
			service.setSSHKey(((Text) widgetRegistry.get(PreferenceConstants.P_SSH_KEY)).getText().trim());
		}
		catch (HerokuServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		super.performDefaults();
		
		try {
			service.setAPIKey( null );
			service.setSSHKey( null );
			
			initialize();
		}
		catch (HerokuServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
