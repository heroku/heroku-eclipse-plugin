package com.heroku.eclipse.ui.views.dialog;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ApplicationInfoPart {
	
	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3,false));
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("Name");
			Text data = new Text(container, SWT.BORDER);
			data.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			Button b = new Button(container, SWT.PUSH);
			b.setText("Rename");
		}
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("URL");
			Label data = new Label(container, SWT.NONE);
			data.setLayoutData(new GridData(GridData.FILL,SWT.CENTER,true,false,2,1));
		}
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("Git Repository URL");
			Label data = new Label(container, SWT.NONE);
			data.setLayoutData(new GridData(GridData.FILL,SWT.CENTER,true,false,2,1));
		}
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("Domain Name");
			Label data = new Label(container, SWT.NONE);
			data.setLayoutData(new GridData(GridData.FILL,SWT.CENTER,true,false,2,1));
		}
		
		return container;
	}
}
