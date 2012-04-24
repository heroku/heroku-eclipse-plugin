package com.heroku.eclipse.ui.preferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import com.heroku.eclipse.ui.Activator;

public class HerokuPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public HerokuPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.HerokuPreferencePage_Title);
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
	protected Control createContents(Composite parent) {
		Composite group = new Composite(parent, SWT.NULL);

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
			l.setText(Messages.HerokuPreferencePage_Email);
			l.setLayoutData(new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());
			t.setLayoutData( new GridData( SWT.FILL, SWT.NONE, true, false, 1, 1 ) );

			@SuppressWarnings("unused")
			Label dummy = new Label(group, SWT.NONE);
		}

		// Password
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.HerokuPreferencePage_Password);
			l.setLayoutData(new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );

			Text t = new Text(group, SWT.PASSWORD | SWT.BORDER );
			
			t.setFont(group.getFont());

			t.setLayoutData(new GridData( SWT.FILL, SWT.NONE, true, false, 1, 1 ));

			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.HerokuPreferencePage_GetAPIKey);
			b.setLayoutData(new GridData( SWT.NONE, SWT.NONE, false, false, 1, 1 ));
		}

		// API Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.HerokuPreferencePage_APIKey);
			l.setLayoutData(new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());

			t.setLayoutData(new GridData( SWT.FILL, SWT.NONE, true, false, 1, 1 ));

			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.HerokuPreferencePage_Validate);
			b.setLayoutData(new GridData( SWT.NONE, SWT.NONE, false, false, 1, 1 ));
		}
		
		// SSH Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.HerokuPreferencePage_SSHKey);
			l.setLayoutData(new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );

			Text t = new Text(group, SWT.BORDER | SWT.MULTI );
			t.setFont(group.getFont());
			t.setLayoutData(new GridData( SWT.FILL, SWT.FILL, false, false, 2, 3 ));
		}
		
		// API Key
		{
			Label l = new Label(group, SWT.NONE);
			l.setText(Messages.HerokuPreferencePage_APIKey);
			l.setLayoutData(new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );

			Text t = new Text(group, SWT.SINGLE | SWT.BORDER);
			t.setFont(group.getFont());

			t.setLayoutData(new GridData( SWT.FILL, SWT.NONE, true, false, 1, 1 ));

			Button b = new Button(group, SWT.NULL);
			b.setText(Messages.HerokuPreferencePage_Validate);
			b.setLayoutData(new GridData( SWT.NONE, SWT.NONE, false, false, 1, 1 ));
		}
		
		// button row
		{
			Button gen = new Button(group, SWT.NULL);
			gen.setText(Messages.HerokuPreferencePage_Validate);
			gen.setLayoutData(new GridData( SWT.NONE, SWT.NONE, false, false, 1, 1 ));

			Button upd = new Button(group, SWT.NULL);
			upd.setText(Messages.HerokuPreferencePage_Validate);
			upd.setLayoutData(new GridData( SWT.NONE, SWT.NONE, false, false, 1, 1 ));
			
			Button clr = new Button(group, SWT.NULL);
			clr.setText(Messages.HerokuPreferencePage_Validate);
			clr.setLayoutData(new GridData( SWT.NONE, SWT.NONE, false, false, 1, 1 ));
		}
		
		return group;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}