/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;

/**
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppImportWizardPage extends WizardPage {
	private HerokuServices service;

	/**
	 * @param pageName
	 */
	protected HerokuAppImportWizardPage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	public HerokuAppImportWizardPage() {
		super("HerokuAppImportWizardPage"); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppImportWizardPage_Title"));
		setTitle(Messages.getString("HerokuAppImportWizardPage_Description"));
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
		HerokuSession session = null;
		// META:
		// #1: ensure valid prefs
		// #2: step1 listAllApps, single select
		// #3: import
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		setControl(composite);

		String sshKey = null;

		// ensure that we have valid prefs
		try {
			session = service.getOrCreateHerokuSession();
			sshKey = service.getSSHKey();
			if (sshKey == null || sshKey.trim().isEmpty()) {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing")); //$NON-NLS-1$
				ErrorDialog.openError(parent.getShell(), Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing_Title"), null, status); //$NON-NLS-1$
				return;
			}
		}
		catch (HerokuServiceException e) {
			if (e.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid")); //$NON-NLS-1$
				ErrorDialog.openError(parent.getShell(), Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid_Title"), null, status); //$NON-NLS-1$
			}
			else if (e.getErrorCode() == HerokuServiceException.NO_API_KEY) {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing")); //$NON-NLS-1$
				ErrorDialog.openError(parent.getShell(), Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing_Title"), null, status); //$NON-NLS-1$
			}
			else {
				e.printStackTrace();
			}
		}

		TableViewer viewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		
		ArrayList<String> dummy = new ArrayList<String>();
		
		Table table = viewer.getTable();
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);

		table.setLayoutData(gd_table);
		table.setHeaderVisible(true);

		{
			TableViewerColumn tableViewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			TableColumn tc = tableViewerColumn.getColumn();
			tc.setWidth(100);
			tc.setText("Name");
			//		tableViewerColumn.setLabelProvider( new GenericMapCellLabelProvider( "{0}", prop.observeDetail( cp.getKnownElements() ) ) ); //$NON-NLS-1$
			// tableViewer.getTable().setSortDirection( SWT.DOWN );

			// widgetRegistry.put( TABLE_COLUMNS.SERVICE_REQUEST_NUMBER, tc );
		}

		{
			TableViewerColumn tableViewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			TableColumn tc = tableViewerColumn.getColumn();
			tc.setWidth(100);
			tc.setText("Git Url");
			//		tableViewerColumn.setLabelProvider( new GenericMapCellLabelProvider( "{0}", prop.observeDetail( cp.getKnownElements() ) ) ); //$NON-NLS-1$
			// tableViewer.getTable().setSortDirection( SWT.DOWN );

			// widgetRegistry.put( TABLE_COLUMNS.SERVICE_REQUEST_NUMBER, tc );
		}

		{
			TableViewerColumn tableViewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			TableColumn tc = tableViewerColumn.getColumn();
			tc.setWidth(100);
			tc.setText("App Url");
			// tableViewerColumn.setLabelProvider( new GenericMapCellLabelProvider( "{0}", prop.observeDetail( cp.getKnownElements() ) ) ); //$NON-NLS-1$
			// tableViewer.getTable().setSortDirection( SWT.DOWN );

			// widgetRegistry.put( TABLE_COLUMNS.SERVICE_REQUEST_NUMBER, tc );
		}

	}
}
