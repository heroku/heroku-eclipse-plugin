package com.heroku.eclipse.ui.views.dialog;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.heroku.api.App;

public class CollaboratorsPart {
	private TableViewer viewer;

	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2,false));
		
		{
			viewer = new TableViewer(container);
			viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
			viewer.getTable().setHeaderVisible(true);
			viewer.getTable().setLinesVisible(true);
			
			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText("Owner");
				column.getColumn().setWidth(200);
			}

			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText("Collaborator E-Mail");
				column.getColumn().setWidth(200);
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
				b.setText("Make Owner");	
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
	
	public void setDomainObject(App domainObject) {
		
	}
}
