/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

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
		setDescription(Messages.getString("HerokuAppImportWizardPage_Title")); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppImportWizardPage_Description")); //$NON-NLS-1$
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

		if (verifyPreferences(composite)) {
			TableViewer viewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			
			List<App> apps = new ArrayList<App>();

			try {
				apps = service.listApps();
			}
			catch (HerokuServiceException e) {
				e.printStackTrace();
				HerokuUtils.internalError(parent.getShell(), e);
			}
			
			viewer.setInput(apps);

			Table table = viewer.getTable();
			GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);

			table.setLayoutData(gd_table);
			table.setHeaderVisible(true);

			{
				TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
				TableColumn tc = vc.getColumn();
				tc.setWidth(100);
				tc.setText(Messages.getString("HerokuAppImportWizardPage_Name")); //$NON-NLS-1$

				vc.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						App app = (App) element;
						return app.getName();
					}
				});
			}

			{
				TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
				TableColumn tc = vc.getColumn();
				tc.setWidth(100);
				tc.setText(Messages.getString("HerokuAppImportWizardPage_GitUrl")); //$NON-NLS-1$

				vc.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						App app = (App) element;
						return app.getGitUrl();
					}
				});
			}

			{
				TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
				TableColumn tc = vc.getColumn();
				tc.setWidth(100);
				tc.setText(Messages.getString("HerokuAppImportWizardPage_AppUrl")); //$NON-NLS-1$
				
				vc.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						App app = (App) element;
						return app.getWebUrl();
					}
				});
			}
		}

	}

	/**
	 * @param parent
	 */
	private boolean verifyPreferences(Composite parent) {
		boolean isOk = true;

		// ensure that we have valid prefs
		String sshKey = null;
		try {
			service.getOrCreateHerokuSession();
			sshKey = service.getSSHKey();

			if (sshKey == null || sshKey.trim().isEmpty()) {
				HerokuUtils
						.userError(
								parent.getShell(),
								Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing_Title"), Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
		}
		catch (HerokuServiceException e) {
			if (e.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
				HerokuUtils.userError(parent.getShell(),
						Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid_Title"), Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			else if (e.getErrorCode() == HerokuServiceException.NO_API_KEY) {
				HerokuUtils
						.userError(
								parent.getShell(),
								Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing_Title"), Messages.getString("HerokuAppImportWizardPage_Error_HerokuPrefsMissing")); //$NON-NLS-1$ //$NON-NLS-2$
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
