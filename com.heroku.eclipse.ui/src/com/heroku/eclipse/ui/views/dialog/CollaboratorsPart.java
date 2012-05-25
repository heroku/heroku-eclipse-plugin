package com.heroku.eclipse.ui.views.dialog;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.LabelProviderFactory;
import com.heroku.eclipse.ui.utils.RunnableWithReturn;
import com.heroku.eclipse.ui.utils.ViewerOperations;

public class CollaboratorsPart {
	private TableViewer viewer;
	private App domainObject;

	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2,false));
		
		{
			viewer = new TableViewer(container);
			viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
			viewer.getTable().setHeaderVisible(true);
			viewer.getTable().setLinesVisible(true);
			viewer.setContentProvider(new ArrayContentProvider());
			
			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText("Owner");
				column.getColumn().pack();
				column.setLabelProvider(LabelProviderFactory.createCollaborator_Owner(new RunnableWithReturn<Boolean, Collaborator>() {
					
					@Override
					public Boolean run(Collaborator argument) {
						return domainObject.getOwnerEmail() == null ? Boolean.FALSE : domainObject.getOwnerEmail().equals(argument.getEmail());
					}
				}));
			}

			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText("Collaborator E-Mail");
				column.getColumn().setWidth(200);
				column.setLabelProvider(LabelProviderFactory.createCollaborator_Email());
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
		try {
			this.domainObject = domainObject; 
			HerokuUtils.runOnDisplay(true, viewer, Activator.getDefault().getService().getCollaborators(domainObject),
					ViewerOperations.input(viewer));
		} catch (HerokuServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
