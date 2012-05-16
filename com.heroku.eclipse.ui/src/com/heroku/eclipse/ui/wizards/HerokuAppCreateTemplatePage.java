/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * 
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuAppCreateTemplatePage extends WizardPage {
	private HerokuServices service;
	
	/**
	 * @param pageName
	 */
	protected HerokuAppCreateTemplatePage(String pageName) {
		super(pageName);
	}

	/**
	 * 
	 */
	public HerokuAppCreateTemplatePage() {
		super("HerokuAppCreateTemplatePage"); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppCreateWizardPage_Title")); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppCreateWizardPage_Description")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening app import wizard"); //$NON-NLS-1$
		
		Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(1, false));
		setControl(group);
		
		group.setEnabled(true);
		setErrorMessage(null);
		setPageComplete(false);

		// ensure valid prefs
		if (!verifyPreferences(group)) {
			Activator.getDefault().getLogger().log(LogService.LOG_INFO, "preferences are missing/invalid"); //$NON-NLS-1$
			group.setEnabled(false);
			setErrorMessage(Messages.getString("Heroku_Common_Error_HerokuPrefsMissing")); //$NON-NLS-1$
		}
		else {
			TableViewer viewer = new TableViewer(group, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			
			Table table = viewer.getTable();
			GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);

			table.setLayoutData(gd_table);
			table.setHeaderVisible(true);

			{
				TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
				TableColumn tc = vc.getColumn();
				tc.setWidth(150);
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
				tc.setWidth(150);
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
			
			List<App> apps = new ArrayList<App>();
			try {
				apps = service.listApps();
			}
			catch (HerokuServiceException e) {
				e.printStackTrace();
				HerokuUtils.internalError(parent.getShell(), e);
			}
			
			if ( apps.size() == 0 ) {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "no applications found"); //$NON-NLS-1$
				setPageComplete(false);
			}
			else {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "found "+apps.size()+" applications, displaying"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			viewer.setInput(apps);
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					setPageComplete(true);
				}
			});
			
		}

	}

	/**
	 * Ensures that the preferences are setup
	 * @param parent
	 * @return true, if the prefs are OK, false if not
	 */
	private boolean verifyPreferences(Composite parent) {
		boolean isOk = true;

		// ensure that we have valid prefs
		try {
			isOk = service.isReady();
		}
		catch (HerokuServiceException e) {
			if (e.getErrorCode() == HerokuServiceException.SECURE_STORE_ERROR) {
				HerokuUtils.userError(parent.getShell(),
						Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid_Title"), Messages.getString("HerokuApp_Common_Error_SecureStoreInvalid")); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			else if (e.getErrorCode() == HerokuServiceException.INVALID_PREFERENCES) {
				HerokuUtils
						.userError(
								parent.getShell(),
								Messages.getString("Heroku_Common_Error_HerokuPrefsMissing_Title"), Messages.getString("Heroku_Common_Error_HerokuPrefsMissing")); //$NON-NLS-1$ //$NON-NLS-2$
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
