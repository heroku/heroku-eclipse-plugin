/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.log.LogService;

import com.heroku.eclipse.core.constants.AppCreateConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
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
	private TableViewer viewer;
	private Label lTemplateName;
	private Text tAddons;
	private Text tFrameworks;
	private Text tDescription;
	private Text tSearch;
	
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
				
				tSearch = new Text(group, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH );
				GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false, 1, 1 );
				tSearch.setLayoutData( gd );
				tSearch.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_SEARCH);
				tSearch.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						viewer.refresh();
						Object o = viewer.getElementAt(0);
						if ( o != null ) {
							viewer.setSelection(new StructuredSelection(o));
						}
					}
				});
				
				tSearch.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if ( e.keyCode == SWT.ARROW_DOWN ) {
							viewer.getControl().setFocus();
						}
					}
				});
				
			}
			
			// template info
			{
				Label lTemplate = new Label(group, SWT.NONE);
				lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_Template")); //$NON-NLS-1$
				
				lTemplateName = new Label( group, SWT.NONE);
				FontData[] fd = lTemplateName.getFont().getFontData();
				fd[0].setStyle(SWT.BOLD);
				lTemplateName.setFont( new Font( lTemplateName.getDisplay(), fd[0] ) );
				GridData gd = new GridData(SWT.LEFT);
				gd.widthHint = 200;
				lTemplateName.setLayoutData(gd);
			}

			// template listing
			{
				viewer = new TableViewer(group, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
				viewer.setContentProvider(ArrayContentProvider.getInstance());
				viewer.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.V_TEMPLATES_LIST);
				viewer.addFilter(new ViewerFilter() {
					
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						if ( tSearch.getText().isEmpty() ) {
							return true;
						}
						else {
							if ( ((AppTemplate)element).getDisplayName().toLowerCase().contains(tSearch.getText().toLowerCase() ) ) {
								return true;
							}
						}
						return false;
					}
				});
				
				Table table = viewer.getTable();
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 6);
				table.setLayoutData(gd);
				table.setHeaderVisible(false);

				{
					TableViewerColumn vc = new TableViewerColumn(viewer, SWT.NONE);
					vc.setLabelProvider(new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							AppTemplate template = (AppTemplate) element;
							return template.getDisplayName();
						}
					});
					TableColumn tc = vc.getColumn();
					tc.setWidth(200);
				}
			}
			
			// template description
			{
				tDescription = new Text( group, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL );
				tDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
				tDescription.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_DESCRIPTION);
				tDescription.setEnabled(false);
			}
			
			// frameworks
			{
				Label lTemplate = new Label(group, SWT.NONE);
				lTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_TemplateFrameworksUsed")); //$NON-NLS-1$
				
				tFrameworks = new Text( group, SWT.BORDER );
				tFrameworks.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				tFrameworks.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_FRAMEWORKS);
				tFrameworks.setEnabled(false);
			}
			
			// addons
			{
				Label lTemplate = new Label(group, SWT.NONE);
				lTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_TemplateAddons")); //$NON-NLS-1$
				
				tAddons = new Text( group, SWT.BORDER);
				tAddons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				tAddons.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_ADDONS);
				tAddons.setEnabled(false);
			}
			
			List<AppTemplate> templates = new ArrayList<AppTemplate>();
			try {
				templates = service.listTemplates();
			}
			catch (HerokuServiceException e) {
				e.printStackTrace();
				HerokuUtils.internalError(parent.getShell(), e);
			}
			
			if ( templates != null && templates.size() == 0 ) {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "no application templates found"); //$NON-NLS-1$
				setPageComplete(false);
			}
			else {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "displaying "+templates.size()+" templates"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			viewer.setInput(templates);
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					final IStructuredSelection s = (IStructuredSelection) event.getSelection();
					displayTemplateDetails((AppTemplate) s.getFirstElement());
					
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

	/**
	 * @param template
	 */
	private void displayTemplateDetails(AppTemplate template) {
		if ( template == null ) {
			lTemplateName.setText(""); //$NON-NLS-1$
			tDescription.setText(""); //$NON-NLS-1$
			tFrameworks.setText(""); //$NON-NLS-1$
		}
		else {
			lTemplateName.setText( template.getDisplayName());
			tDescription.setText(template.getDisplayName());
			tFrameworks.setText(template.getLanguage());
		}
		lTemplateName.getParent().layout();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if ( visible ) {
			tSearch.setFocus();
		}
	}
}
