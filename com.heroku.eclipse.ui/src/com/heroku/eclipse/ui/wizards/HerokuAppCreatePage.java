/**
 * 
 */
package com.heroku.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

import com.heroku.eclipse.core.constants.AppCreateConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;

/**
 * Wizard page displaying everything needed to create a new Heroku App
 * @author udo.rader@bestsolution.at
 * 
 */
public class HerokuAppCreatePage extends WizardPage {
	private HerokuServices service;
	
	private Text tAppName;
	private TableViewer viewer;
	private Label lTemplateName;
	private Text tAddons;
	private Text tFrameworks;
	private Text tDescription;
	private Text tSearch;

	private AppTemplate appTemplate;

	/**
	 * 
	 */
	public HerokuAppCreatePage() {
		super("HerokuAppCreatePage"); //$NON-NLS-1$
		setTitle(Messages.getString("HerokuAppCreateTemplatePage_Title")); //$NON-NLS-1$
		setDescription(Messages.getString("HerokuAppCreateTemplatePage_Description")); //$NON-NLS-1$
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		
		setImageDescriptor(ImageDescriptor.createFromURL(FileLocator.find(bundle,new Path("icons/55_45/heroku_logo_55_45.png"),
				                                   						  null)
				                          )
				           );
		service = Activator.getDefault().getService();
	}
	
	@Override
	public void createControl(Composite parent) {
		Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "opening app create wizard, page"); //$NON-NLS-1$

		final Composite group = new Composite(parent, SWT.NONE);
		group.setLayout(new GridLayout(2, true));
		setErrorMessage(null);
		setPageComplete(false);
		
		// app name
		{
			Composite name = new Composite(group, SWT.NONE);
			name.setLayout(new GridLayout(2, false));
			name.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			Label l = new Label(name, SWT.NONE);
			l.setText(Messages.getString("HerokuAppCreateNamePage_Name")); //$NON-NLS-1$

			tAppName = new Text(name, SWT.BORDER);
			tAppName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
			tAppName.setTextLimit(30);
			tAppName.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.C_APP_NAME);
			tAppName.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					setErrorMessage(null);
					if ( HerokuUtils.isNotEmpty(tAppName.getText()) && ! service.isAppNameBasicallyValid(tAppName.getText().toLowerCase())) {
						setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists_Hint")); //$NON-NLS-1$
					}
					setPageComplete(isPageComplete());
				}
			});
		}
		
		Composite left = new Composite(group, SWT.FILL);
		left.setLayout(new GridLayout(2, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Composite right = new Composite(group, SWT.FILL);
		right.setLayout(new GridLayout(2, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// search
		{
			Label lSearch = new Label(left, SWT.NONE);
			lSearch.setText(Messages.getString("HerokuAppCreateTemplatePage_Search")); //$NON-NLS-1$

			tSearch = new Text(left, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			tSearch.setLayoutData(gd);
			tSearch.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_SEARCH);
			tSearch.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					viewer.refresh();
					Object o = viewer.getElementAt(0);
					if (o != null) {
						viewer.setSelection(new StructuredSelection(o));
					}
				}
			});

			tSearch.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.keyCode == SWT.ARROW_DOWN) {
						viewer.getControl().setFocus();
					}
				}
			});

		}

		// template info
		{
			Label lTemplate = new Label(right, SWT.NONE);
			lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_Template")); //$NON-NLS-1$

			lTemplateName = new Label(right, SWT.NONE);
			FontData[] fd = lTemplateName.getFont().getFontData();
			fd[0].setStyle(SWT.BOLD);
			lTemplateName.setFont(new Font(lTemplateName.getDisplay(), fd[0]));
			GridData gd = new GridData(SWT.LEFT);
			gd.widthHint = 200;
			lTemplateName.setLayoutData(gd);
		}

		// template listing
		{
			viewer = new TableViewer(left, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.getTable().setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.V_TEMPLATES_LIST);
			viewer.addFilter(new ViewerFilter() {

				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (tSearch.getText().isEmpty()) {
						return true;
					}
					else {
						if (((AppTemplate) element).getDisplayName().toLowerCase().contains(tSearch.getText().toLowerCase())) {
							return true;
						}
					}
					return false;
				}
			});

			Table table = viewer.getTable();
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 7);
			gd.heightHint = 300;
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
			tDescription = new Text(right, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			tDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 3));
			tDescription.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_DESCRIPTION);
			tDescription.setEnabled(false);
		}

		// frameworks
		{
			Label lTemplate = new Label(right, SWT.NONE);
			lTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
			lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_TemplateFrameworksUsed")); //$NON-NLS-1$

			tFrameworks = new Text(right, SWT.BORDER);
			tFrameworks.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			tFrameworks.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_FRAMEWORKS);
			tFrameworks.setEnabled(false);
		}

		// addons
		{
			Label lTemplate = new Label(right, SWT.NONE);
			lTemplate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
			lTemplate.setText(Messages.getString("HerokuAppCreateTemplatePage_TemplateAddons")); //$NON-NLS-1$

			tAddons = new Text(right, SWT.BORDER);
			tAddons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			tAddons.setData(HerokuServices.ROOT_WIDGET_ID, AppCreateConstants.T_ADDONS);
			tAddons.setEnabled(false);
		}

		// async templates fetching
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					group.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							try {
								List<AppTemplate> templates = service.listTemplates(monitor);

								if (templates == null || templates.size() == 0) {
									Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "no application templates found"); //$NON-NLS-1$
									setPageComplete(false);
								}
								else {
									Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "displaying " + templates.size() + " templates"); //$NON-NLS-1$ //$NON-NLS-2$
								}

								viewer.setInput(templates);
							}
							catch (HerokuServiceException e) {
								if (e.getErrorCode() == HerokuServiceException.REQUEST_FAILED) {
									HerokuUtils.herokuError(group.getShell(), e);
								}
								else {
									HerokuUtils.internalError(group.getShell(), e);
								}
							}
						}
					});
				}
			});
		}
		catch (InvocationTargetException e1) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unable to fetch templates due to unknown internal error!"); //$NON-NLS-1$
			HerokuUtils.internalError(parent.getShell(), e1);
		}
		catch (InterruptedException e1) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unable to fetch templates due to unknown internal error!"); //$NON-NLS-1$
			HerokuUtils.internalError(parent.getShell(), e1);
		}

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final IStructuredSelection s = (IStructuredSelection) event.getSelection();
				appTemplate = (AppTemplate) s.getFirstElement();
				displayTemplateDetails(appTemplate);
				
				setPageComplete(isPageComplete());
			}
		});
		
		setControl(group);
		group.setEnabled(true);
	}
	
	@Override
	public boolean isPageComplete() {
		if ( HerokuUtils.isNotEmpty(tAppName.getText()) && ! service.isAppNameBasicallyValid(tAppName.getText().toLowerCase())) {
			return false;
		}
		
		return getAppTemplate() != null ? true : false;
	}

	/**
	 * @param template
	 */
	private void displayTemplateDetails(AppTemplate template) {
		if (template == null) {
			lTemplateName.setText(""); //$NON-NLS-1$
			tDescription.setText(""); //$NON-NLS-1$
			tFrameworks.setText(""); //$NON-NLS-1$
		}
		else {
			lTemplateName.setText(template.getDisplayName());
			tDescription.setText(template.getDisplayName());
			tFrameworks.setText(template.getLanguage());
		}
		lTemplateName.getParent().layout();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			tAppName.setFocus();
		}
	}
	
	/**
	 * Delivers the AppTemplate instance of the chosen template
	 * 
	 * @return the AppTemplate instance of the chosen template
	 */
	public AppTemplate getAppTemplate() {
		return appTemplate;
	}

	/**
	 * Delivers the chose name of the new app
	 * 
	 * @return the name of the new application to create
	 */
	public String getAppName() {
		return HerokuUtils.ensureNotNull(tAppName.getText()).trim();
	}
	
	/**
	 * Displays a warning about an invalid/duplicate app name
	 */
	public void displayInvalidNameWarning() {
		setVisible(true);
		setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_NameAlreadyExists")); //$NON-NLS-1$
		tAppName.setFocus();
	}
	
	/**
	 * Displays a warning about an unverified account
	 */
	public void displayUnverifiedAccountWarning() {
		setVisible(true);
		setErrorMessage(Messages.getString("HerokuAppCreateNamePage_Error_UnverifiedAccount")); //$NON-NLS-1$
		tAppName.setFocus();
	}
}
