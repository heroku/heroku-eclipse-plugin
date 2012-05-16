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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
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
		setTitle(Messages.getString("HerokuAppCreateTemplatePage_Title")); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppCreateTemplatePage_Description")); //$NON-NLS-1$
		service = Activator.getDefault().getService();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening app create wizard, templates listing page"); //$NON-NLS-1$
		
		Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(4, false));
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
			// search
			{
				Label lSearch = new Label(group, SWT.NONE);
				lSearch.setText(Messages.getString("HerokuAppCreateTemplatePage_Search")); //$NON-NLS-1$
				
				final Text tSearch = new Text(group, SWT.BORDER);
				tSearch.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false, 1, 1 ) );
				tSearch.setTextLimit( 100 );
				tSearch.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						setErrorMessage(null);
						setPageComplete(true);
						if ( tSearch.getText() != null || ! tSearch.getText().trim().isEmpty() ) {
							// TODO search ...
						}
					}
				});
			}
			
			// template info
			{
				Label lTemplate = new Label(group, SWT.NONE);
				lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_Template")); //$NON-NLS-1$
				
				Label lTemplateName = new Label( group, SWT.BOLD);
				lTemplateName.setText("foobar Template");
			}

			// template listing
			{
				TableViewer viewer = new TableViewer(group, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
				viewer.setContentProvider(ArrayContentProvider.getInstance());
				
				Table table = viewer.getTable();
				table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 6));
				table.setHeaderVisible(false);
				{
					TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
					vc.setLabelProvider(new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							App app = (App) element;
							return app.getName();
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
			
			// template description
			{
				Text t = new Text( group, SWT.BORDER );
				t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				t.setText("Template description");
				t.setEnabled(false);
			}
			
			// frameworks
			{
				Label lTemplate = new Label(group, SWT.NONE);
				lTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_TemplateFrameworksUsed")); //$NON-NLS-1$
				
				Text t = new Text( group, SWT.BORDER );
				t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				t.setText("frameworks listing");
				t.setEnabled(false);
			}
			
			// addons
			{
				Label lTemplate = new Label(group, SWT.NONE);
				lTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_TemplateAddons")); //$NON-NLS-1$
				
				Text t = new Text( group, SWT.BORDER);
				t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				t.setText("addon listing");
				t.setEnabled(false);
			}
			
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
