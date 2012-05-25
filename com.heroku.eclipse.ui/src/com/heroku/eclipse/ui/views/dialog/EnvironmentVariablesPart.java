package com.heroku.eclipse.ui.views.dialog;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class EnvironmentVariablesPart {
	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2,false));
		
		{
			TableViewer viewer = new TableViewer(container);
			
			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText("Key");
			}
			
			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText("Value");
			}
		}
		
		{
			Composite controls = new Composite(container, SWT.NONE);
			controls.setLayout(new GridLayout(1,true));
			
			{
				Button b = new Button(controls, SWT.PUSH);
				b.setText("+");	
				b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}
			
			{
				Button b = new Button(controls, SWT.PUSH);
				b.setText("-");
				b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}
			
			{
				Button b = new Button(controls, SWT.PUSH);
				b.setText("Save");	
				b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}
		}
		
		return container;
	}
}