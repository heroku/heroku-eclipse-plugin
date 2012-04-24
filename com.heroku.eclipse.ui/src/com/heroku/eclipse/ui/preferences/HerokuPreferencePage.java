package com.heroku.eclipse.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;

public class HerokuPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String HEROKU_PREFERENCE_PAGE_CONTEXT = "com.heroku.eclipse.heroclipse_context"; //$NON-NLS-1$
	
	private Map<String, Object>	widgetRegistry = new HashMap<String, Object>();
	private Map<String, ControlDecoration>	decoratorRegistry = new HashMap<String, ControlDecoration>();
	
	public HerokuPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.getString( "HerokuPreferencePage_Title" ) );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(final Composite parent) {
		
		final HerokuServices service = Activator.getDefault().getService();

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
			l.setText(Messages.getString( "HerokuPreferencePage_Email" ) );
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
					1, 1));

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());
			t.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
			
			ControlDecoration c = new ControlDecoration( t, SWT.BOTTOM | SWT.LEFT );
			c.setImage( FieldDecorationRegistry.getDefault().getFieldDecoration( FieldDecorationRegistry.DEC_ERROR ).getImage() );
			c.setDescriptionText( Messages.getString( "HerokuPreferencePage_Error_Decorator_EmailMissing" ) ); //$NON-NLS-1$
			c.hide();
			
			widgetRegistry.put( PreferenceConstants.P_EMAIL, t );
			decoratorRegistry.put( PreferenceConstants.P_EMAIL, c );
			
			@SuppressWarnings("unused")
			Label dummy = new Label(group, SWT.NONE);
		}

		// Password
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString( "HerokuPreferencePage_Password" ) );
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
					1, 1));

			Text t = new Text(group, SWT.PASSWORD | SWT.BORDER);
			t.setFont(group.getFont());
			t.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
			
			ControlDecoration c = new ControlDecoration( t, SWT.BOTTOM | SWT.LEFT );
			c.setImage( FieldDecorationRegistry.getDefault().getFieldDecoration( FieldDecorationRegistry.DEC_ERROR ).getImage() );
			c.setDescriptionText( Messages.getString( "HerokuPreferencePage_Error_Decorator_PasswordMissing" ) ); //$NON-NLS-1$
			c.hide();
			
			widgetRegistry.put( PreferenceConstants.P_PASSWORD, t );
			decoratorRegistry.put( PreferenceConstants.P_PASSWORD, c );

			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.getString( "HerokuPreferencePage_GetAPIKey" ) );
			b.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			b.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if ( !validateLoginData( true ) ) {
						((Button) widgetRegistry.get( PreferenceConstants.P_VALIDATE_API_KEY )).setEnabled( false );
					}
					else {
						final AtomicReference<String> apiKey = new AtomicReference<String>();
						
						final String email = ((Text) widgetRegistry.get( PreferenceConstants.P_EMAIL )).getText();
						final String password = ((Text) widgetRegistry.get( PreferenceConstants.P_PASSWORD )).getText();
						
						try {
							PlatformUI.getWorkbench().getProgressService().busyCursorWhile( new IRunnableWithProgress() {
//							PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
								
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException,
										InterruptedException {
									try {
										apiKey.set( service.getAPIKey( email, password ) );
									} catch (HerokuServiceException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										throw new InvocationTargetException( e );
									}
								}
							});
						} catch (InvocationTargetException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						// login failed
						if ( apiKey.get() == null ) {
							MessageDialog.openError(getShell(), Messages.getString( "HerokuPreferencePage_Error_LoginFailed_Title" ), Messages.getString( "HerokuPreferencePage_Error_LoginFailed" ) );
						}
						else {
							((Button) widgetRegistry.get( PreferenceConstants.P_VALIDATE_API_KEY )).setEnabled( true );
							// TODO: set API key 
						}
					}
				}
			});
		}

		// API Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString( "HerokuPreferencePage_APIKey" ) );
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,
					1, 1));

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());
			t.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
			
			ControlDecoration c = new ControlDecoration( t, SWT.BOTTOM | SWT.LEFT );
			c.setImage( FieldDecorationRegistry.getDefault().getFieldDecoration( FieldDecorationRegistry.DEC_ERROR ).getImage() );
			c.setDescriptionText( Messages.getString( "HerokuPreferencePage_Error_Decorator_APIKeyMissing" ) ); //$NON-NLS-1$
			c.hide();
			
			widgetRegistry.put( PreferenceConstants.P_API_KEY, t );
			decoratorRegistry.put( PreferenceConstants.P_API_KEY, c );
			
			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.getString( "HerokuPreferencePage_Validate" ) );
			b.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1, 1));
			// only enable if we have valid login data
			b.setEnabled( validateLoginData( false ) );
			b.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if ( validateAPIKeyData( true ) ) {
						// TODO "list apps" w/o error
					}
				}

			});
			
			widgetRegistry.put( PreferenceConstants.P_VALIDATE_API_KEY, b );
		}

		// SSH Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.getString( "HerokuPreferencePage_SSHKey" ) );
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));

			Text t = new Text(group, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL
					| SWT.V_SCROLL);
			t.setFont(group.getFont());
			GridData g = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
			g.heightHint = 50;
			t.setLayoutData(g);
			
			widgetRegistry.put( PreferenceConstants.P_SSH_KEY, t );

		}

		// button row
		{
			Composite right = new Composite(group, SWT.NONE);
			right.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false,
					3, 1));

			GridLayout gl = new GridLayout(3, true);

			gl.marginHeight = 0;
			gl.marginWidth = 0;

			right.setLayout(gl);

			Button gen = new Button(right, SWT.NULL);
			gen.setText(Messages.getString( "HerokuPreferencePage_Generate" ) );
			gen.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1,
					1));
			gen.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					// TODO utilize eclipse keygen
				}

			});

			Button upd = new Button(right, SWT.NULL);
			upd.setText(Messages.getString( "HerokuPreferencePage_Update" ) );
			upd.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1,
					1));
			upd.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					// TODO call /keys/add
				}

			});

			Button clr = new Button(right, SWT.NULL);
			clr.setText(Messages.getString( "HerokuPreferencePage_Clear" ) );
			clr.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false, 1,
					1));
			clr.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					// TODO remove from heroku account?
				}

			});
		}

		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(getControl(), HEROKU_PREFERENCE_PAGE_CONTEXT);

		return group;
	}

	/**
	 * Verifies that all data required for login is present
	 * @param decorate if set to true, the relevant fields will be decorated with an error icon
	 * @return
	 */
	private boolean validateLoginData( boolean decorate ) {
		boolean isValid = true;
		// validate Email/Password
		Text email = (Text) widgetRegistry.get( PreferenceConstants.P_EMAIL );
		Text password = (Text) widgetRegistry.get( PreferenceConstants.P_PASSWORD );
		
		if ( email.getText().trim().isEmpty() ) {
			if ( decorate ) {
				decoratorRegistry.get( PreferenceConstants.P_EMAIL ).show();
			}
			isValid = false;
		}
		else {
			decoratorRegistry.get( PreferenceConstants.P_EMAIL ).hide();
		}
		
		if ( password.getText().trim().isEmpty() ) {
			if ( decorate ) {
				decoratorRegistry.get( PreferenceConstants.P_PASSWORD ).show();
			}
			isValid = false;
		}
		else {
			if ( decorate ) {
				decoratorRegistry.get( PreferenceConstants.P_PASSWORD ).hide();
			}
		}
		return isValid;
	}

	/**
	 * Validates that all the data required for validating an API key is there 
	 * @param decorate if set to true, the relevant fields will be decorated with an error icon
	 * @return
	 */
	private boolean validateAPIKeyData( boolean decorate ) {
		// validate API key
		boolean isValid = true;
		
		Text t = (Text) widgetRegistry.get( PreferenceConstants.P_API_KEY );
		
		if ( t.getText().trim().isEmpty() ) {
			if ( decorate ) {
				decoratorRegistry.get( PreferenceConstants.P_API_KEY ).show();
			}
			isValid = false;
		}
		else {
			decoratorRegistry.get( PreferenceConstants.P_API_KEY ).hide();
		}
		return isValid;
	}
	
}