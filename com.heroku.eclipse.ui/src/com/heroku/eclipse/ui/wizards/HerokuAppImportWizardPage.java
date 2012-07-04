/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.constants.AppImportConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuServices.APP_FIELDS;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.AppComparator;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.LabelProviderFactory;

/**
 * 
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppImportWizardPage extends WizardPage {
	private HerokuServices service;
	private App app = null;
	private TableViewerColumn nameColumn;
	private TableViewer viewer;

	/**
	 * 
	 */
	public HerokuAppImportWizardPage() {
		super("HerokuAppImportWizardPage"); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppImportWizardPage_Title")); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppImportWizardPage_Description")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}

	@Override
	public void createControl(Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening app import wizard"); //$NON-NLS-1$

		Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(1, false));
		setControl(group);

		group.setEnabled(true);
		setErrorMessage(null);
		setPageComplete(false);

		viewer = new TableViewer(group, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setData(HerokuServices.ROOT_WIDGET_ID, AppImportConstants.V_APPS_LIST);
		viewer.setComparator(new AppComparator());

		Table table = viewer.getTable();
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd_table.heightHint = table.getItemHeight() * 15;
		table.setLayoutData(gd_table);
		table.setHeaderVisible(true);

		{
			nameColumn = new TableViewerColumn(viewer, SWT.NONE);
			nameColumn.setLabelProvider(LabelProviderFactory.createApp_Name());

			TableColumn tc = nameColumn.getColumn();
			tc.setWidth(150);
			tc.setText(Messages.getString("HerokuAppImportWizardPage_Name")); //$NON-NLS-1$
			tc.setData(AppComparator.SORT_IDENTIFIER, APP_FIELDS.APP_NAME);
			tc.addSelectionListener(getSelectionAdapter(tc));
		}

		{
			TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
			vc.setLabelProvider(LabelProviderFactory.createApp_GitUrl());

			TableColumn tc = vc.getColumn();
			tc.setWidth(150);
			tc.setText(Messages.getString("HerokuAppImportWizardPage_GitUrl")); //$NON-NLS-1$
			tc.setData(AppComparator.SORT_IDENTIFIER, APP_FIELDS.APP_GIT_URL);
			tc.addSelectionListener(getSelectionAdapter(tc));

		}

		{
			TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
			vc.setLabelProvider(LabelProviderFactory.createApp_Url());

			TableColumn tc = vc.getColumn();
			tc.setWidth(100);
			tc.setText(Messages.getString("HerokuAppImportWizardPage_AppUrl")); //$NON-NLS-1$
			tc.setData(AppComparator.SORT_IDENTIFIER, APP_FIELDS.APP_WEB_URL);
			tc.addSelectionListener(getSelectionAdapter(tc));

		}

		List<App> apps = new ArrayList<App>();
		try {
			apps = service.listApps();
		}
		catch (HerokuServiceException e) {
			HerokuUtils.internalError(parent.getShell(), e);
		}

		if (apps.size() == 0) {
			Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "no applications found"); //$NON-NLS-1$
			setPageComplete(false);
		}
		else {
			Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "found " + apps.size() + " applications, displaying"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		viewer.setInput(apps);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final IStructuredSelection s = (IStructuredSelection) event.getSelection();
				app = (App) s.getFirstElement();
				setPageComplete(true);
			}
		});

		viewer.getTable().setSortColumn(nameColumn.getColumn());
		viewer.getTable().setSortDirection(SWT.UP);
		viewer.refresh();
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				int dir = viewer.getTable().getSortDirection();
				if (viewer.getTable().getSortColumn() == column) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				}
				else {
					dir = SWT.DOWN;
				}

				viewer.getTable().setSortColumn(column);
				viewer.getTable().setSortDirection(dir);
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}

	@Override
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}
	
	/**
	 * Returns the selected app
	 * 
	 * @return the selected app
	 */
	public App getSelectedApp() {
		return app;
	}
}
