package com.heroku.eclipse.ui.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.LabelProviderFactory;
import com.heroku.eclipse.ui.utils.ViewerOperations;
import com.heroku.eclipse.ui.views.dialog.ApplicationInfoPart;
import com.heroku.eclipse.ui.views.dialog.CollaboratorsPart;
import com.heroku.eclipse.ui.views.dialog.EnvironmentVariablesPart;

/**
 * The main view of the Heroclipse plugin
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuApplicationManagerViewPart extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.heroku.eclipse.ui.views.HerokuApplicationManager"; //$NON-NLS-1$

	private TableViewer viewer;

	private HerokuServices herokuService;

	private List<ServiceRegistration<EventHandler>> handlerRegistrations;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		herokuService = Activator.getDefault().getService();
	}

	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);

		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("App Status");
			column.setLabelProvider(LabelProviderFactory.createApp_Status());
			column.getColumn().pack();
		}

		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("Name");
			column.setLabelProvider(LabelProviderFactory.createApp_Name());
			column.getColumn().setWidth(200);
		}

		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("Git Url");
			column.setLabelProvider(LabelProviderFactory.createApp_GitUrl());
			column.getColumn().setWidth(200);
		}

		{
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText("App Url");
			column.setLabelProvider(LabelProviderFactory.createApp_Url());
			column.getColumn().setWidth(200);
		}
		
		{
			MenuManager mgr = createContextMenu();
			Menu menu = mgr.createContextMenu(parent);
			viewer.getControl().setMenu(menu);			
		}
		
		refreshApplications();
		subscribeToEvents();
	}
	
	App getSelectedApp() {
		IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
		return ! s.isEmpty() ? (App) s.getFirstElement() : null;  
	}

	Shell getShell() {
		return getSite().getWorkbenchWindow().getShell();
	}
	
	private MenuManager createContextMenu() {
		Action refresh = new Action("Refresh") {
			@Override
			public void run() {
				refreshApplications();
			}
		};
		
		final Action importApp = new Action("Import") {
			@Override
			public void run() {
				
			}
		};
		
		final Action open = new Action("Open") {
			@Override
			public void run() {
				App app = getSelectedApp();
				if( app != null ) {
					try {
						IWorkbenchBrowserSupport wbb = getSite().getWorkbenchWindow().getWorkbench().getBrowserSupport();
						IWebBrowser browser = wbb.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, app.getName(), app.getName(), "Heroku application - " + app.getName());
						browser.openURL(new URL(app.getWebUrl()));
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				}
			}
		};
		
		final Action restart = new Action("Restart") {
			@Override
			public void run() {
				App app = getSelectedApp();
				if( app != null ) {
					if( MessageDialog.openQuestion(getShell(), "Restart", "Would you really like to restart '"+app.getName()+"'?") ) {
						try {
							herokuService.restartApplication(app);
						} catch (HerokuServiceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		};
		
		final Action viewLogs = new Action("View Logs") {
			@Override
			public void run() {
				
			}
		};
		
		final Action scale = new Action("Scale") {
			@Override
			public void run() {
				
			}
		};
		
		final Action destroy = new Action("Destroy") {
			@Override
			public void run() {
				App app = getSelectedApp();
				if( app != null ) {
					if( MessageDialog.openQuestion(getShell(), "Restart", "Would you really like to destroy '"+app.getName()+"'?") ) {
						try {
							herokuService.destroyApplication(app);
						} catch (HerokuServiceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		};
		
		MenuManager mgr = new MenuManager();
		mgr.add(refresh);
		mgr.add(new Separator());
		mgr.add(importApp);
		mgr.add(open);
		mgr.add(restart);
		mgr.add(viewLogs);
		mgr.add(scale);
		mgr.add(destroy);
		mgr.addMenuListener(new IMenuListener() {
			
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
				
				boolean enabled = !s.isEmpty();
				importApp.setEnabled(enabled);
				open.setEnabled(enabled);
				restart.setEnabled(enabled);
				viewLogs.setEnabled(enabled);
				scale.setEnabled(enabled);
				destroy.setEnabled(enabled);
				
				if( ! s.isEmpty() ) {
					App app = (App) s.getFirstElement();
					
				} else {
					
				}
			}
		});
		
		return mgr;
	}
	
	private void subscribeToEvents() {
		handlerRegistrations = new ArrayList<ServiceRegistration<EventHandler>>();
		EventHandler sessionInvalidationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(
				sessionInvalidationHandler,
				HerokuServices.TOPIC_SESSION_INVALID));

		EventHandler newApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(
				newApplicationHandler, HerokuServices.TOPIC_APPLICATION_NEW));
	}

	private void refreshApplications() {
		try {
			if (herokuService.canObtainHerokuSession()) {
				HerokuUtils.runOnDisplay(true, viewer,
						herokuService.listApps(),
						ViewerOperations.input(viewer));
			} else {
				HerokuUtils.runOnDisplay(true, viewer, new Object[0],
						ViewerOperations.input(viewer));
			}
		} catch (HerokuServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		if (handlerRegistrations != null) {
			for (ServiceRegistration<EventHandler> r : handlerRegistrations) {
				r.unregister();
			}
		}
		super.dispose();
	}

	static class DialogImpl extends TitleAreaDialog {

		public DialogImpl(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);

			CTabFolder folder = new CTabFolder(container, SWT.BOTTOM
					| SWT.BORDER);

			{
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("Application Info");
				ApplicationInfoPart infopart = new ApplicationInfoPart();
				infopart.createUI(folder);
			}

			{
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("Collaborators");
				CollaboratorsPart collabpart = new CollaboratorsPart();
				collabpart.createUI(folder);
			}

			{
				CTabItem item = new CTabItem(folder, SWT.NONE);
				item.setText("Environment Variables");
				EnvironmentVariablesPart envpart = new EnvironmentVariablesPart();
				envpart.createUI(folder);
			}

			return container;
		}
	}
}