package com.heroku.eclipse.ui.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
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
import com.heroku.api.Proc;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.LabelProviderFactory;
import com.heroku.eclipse.ui.utils.RunnableWithReturn;
import com.heroku.eclipse.ui.utils.ViewerOperations;
import com.heroku.eclipse.ui.views.dialog.WebsiteOpener;

/**
 * The main view of the Heroclipse plugin
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuApplicationManagerViewPart extends ViewPart implements WebsiteOpener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.heroku.eclipse.ui.views.HerokuApplicationManager"; //$NON-NLS-1$

	private TreeViewer viewer;

	private HerokuServices herokuService;

	private List<ServiceRegistration<EventHandler>> handlerRegistrations;

	private Map<String, List<Proc>> appProcesses = new HashMap<String, List<Proc>>();
	
	private Timer refreshTimer = new Timer(true);

	private TimerTask refreshTask;
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);

		herokuService = Activator.getDefault().getService();
	}

	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ContentProviderImpl());
		viewer.getTree().setHeaderVisible(true);
		viewer.getTree().setLinesVisible(true);
		viewer.setComparer(new ElementComparerImpl());

		{
			TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(Messages.getString("HerokuAppManagerViewPart_Name")); //$NON-NLS-1$
			column.setLabelProvider(LabelProviderFactory.createName(new RunnableWithReturn<List<Proc>, App>() {
				
				@Override
				public List<Proc> run(App argument) {
					return appProcesses.get(argument.getId());
				}
			}));
			column.getColumn().setWidth(200);
		}

		{
			TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(Messages.getString("HerokuAppManagerViewPart_GitUrl")); //$NON-NLS-1$
			column.setLabelProvider(LabelProviderFactory.createApp_GitUrl());
			column.getColumn().setWidth(200);
		}

		{
			TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(Messages.getString("HerokuAppManagerViewPart_AppUrl")); //$NON-NLS-1$
			column.setLabelProvider(LabelProviderFactory.createApp_Url());
			column.getColumn().setWidth(200);
		}

		{
			MenuManager mgr = createContextMenu();
			Menu menu = mgr.createContextMenu(parent);
			viewer.getControl().setMenu(menu);
		}

		viewer.addOpenListener(new IOpenListener() {

			@Override
			public void open(OpenEvent event) {
				App app = getSelectedApp();
				if (app != null) {
					try {
						getSite().getWorkbenchWindow().getActivePage().openEditor(
								new ApplicationEditorInput(app), ApplicationInfoEditor.ID, true);
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				}
			}
		});

		refreshApplications();
		subscribeToEvents();
	}

	private void scheduleRefresh() {
		refreshTask = new TimerTask() {
			
			@Override
			public void run() {
				refreshApplications();
			}
		};
		refreshTimer.schedule(refreshTask, 20000);
	}
	
	App getSelectedApp() {
		IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
		
		return !s.isEmpty() && s.getFirstElement() instanceof App ? (App) s.getFirstElement() : null;
	}

	Shell getShell() {
		return getSite().getWorkbenchWindow().getShell();
	}

	private MenuManager createContextMenu() {
		Action refresh = new Action(Messages.getString("HerokuAppManagerViewPart_Refresh")) { //$NON-NLS-1$
			@Override
			public void run() {
				refreshApplications();
			}
		};

		final Action importApp = new Action(Messages.getString("HerokuAppManagerViewPart_Import")) { //$NON-NLS-1$
			@Override
			public void run() {

			}
		};

		final Action open = new Action(Messages.getString("HerokuAppManagerViewPart_Open")) { //$NON-NLS-1$
			@Override
			public void run() {
				App app = getSelectedApp();
				if (app != null) {
					openInternal(app);
				}
			}
		};

		final Action restart = new Action(Messages.getString("HerokuAppManagerViewPart_Restart")) { //$NON-NLS-1$
			@Override
			public void run() {
				App app = getSelectedApp();
				if (app != null) {
					if (MessageDialog.openQuestion(getShell(), Messages.getString("HerokuAppManagerViewPart_Restart"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_Restart", app.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							herokuService.restartApplication(app);
						}
						catch (HerokuServiceException e) {
							e.printStackTrace();
							HerokuUtils.internalError(getShell(), e);
						}
					}
				}
			}
		};

		final Action viewLogs = new Action(Messages.getString("HerokuAppManagerViewPart_ViewLogs")) { //$NON-NLS-1$
			@Override
			public void run() {

			}
		};

		final Action scale = new Action(Messages.getString("HerokuAppManagerViewPart_Scale")) { //$NON-NLS-1$
			@Override
			public void run() {

			}
		};

		final Action destroy = new Action(Messages.getString("HerokuAppManagerViewPart_Destroy")) { //$NON-NLS-1$
			@Override
			public void run() {
				App app = getSelectedApp();
				if (app != null) {
					if (MessageDialog.openQuestion(getShell(), Messages.getString("HerokuAppManagerViewPart_Destroy"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_Destroy", app.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							herokuService.destroyApplication(app);
						}
						catch (HerokuServiceException e) {
							e.printStackTrace();
							HerokuUtils.internalError(getShell(), e);
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
			}
		});

		return mgr;
	}

	@Override
	public void openInternal(App application) {
		try {
			IWorkbenchBrowserSupport wbb = getSite().getWorkbenchWindow().getWorkbench().getBrowserSupport();
			IWebBrowser browser = wbb.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, application.getName(), application.getName(),
					Messages.getFormattedString("HerokuAppManagerViewPart_HerokuApp", //$NON-NLS-1$
							application.getName()));
			browser.openURL(new URL(application.getWebUrl()));
		}
		catch (PartInitException e) {
			e.printStackTrace();
			HerokuUtils.internalError(getShell(), e);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			HerokuUtils.internalError(getShell(), e);
		}
	}

	private void subscribeToEvents() {

		EventHandler sessionInvalidationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};

		EventHandler newApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};

		EventHandler renameApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};

		EventHandler transferApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};
		
		EventHandler destroyedApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications();
			}
		};

		handlerRegistrations = new ArrayList<ServiceRegistration<EventHandler>>();
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(sessionInvalidationHandler, HerokuServices.TOPIC_SESSION_INVALID));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(newApplicationHandler, HerokuServices.TOPIC_APPLICATION_NEW));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(renameApplicationHandler, HerokuServices.TOPIC_APPLICATION_RENAMED));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(transferApplicationHandler, HerokuServices.TOPIC_APPLICATION_TRANSFERED));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(destroyedApplicationHandler, HerokuServices.TOPIC_APPLICATION_DESTROYED));
	}

	private void refreshApplications() {
		final Job o = new Job(Messages.getString("HerokuAppManagerViewPart_RefreshApps")) { //$NON-NLS-1$
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					saveRefreshApplications();	
				} catch (Throwable e) {
					HerokuUtils.internalError(getShell(), e);
				}
				
				return Status.OK_STATUS;
			}
		};
		o.schedule();
	}
	
	private void saveRefreshApplications() {
		try {
			appProcesses.clear();
			if (herokuService.isReady()) {
				List<App> applications = herokuService.listApps();
				for( App a : applications ) {
					appProcesses.put(a.getId(), herokuService.listProcesses(a));
				}
				HerokuUtils.runOnDisplay(true, viewer, applications, ViewerOperations.input(viewer));
			}
			else {
				HerokuUtils.runOnDisplay(true, viewer, new Object[0], ViewerOperations.input(viewer));
			}
			
			if( refreshTask != null ) {
				refreshTask.cancel();
			}
			
			scheduleRefresh();
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			HerokuUtils.internalError(getShell(), e);
		}
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		appProcesses.clear();
		refreshTimer.cancel();
		
		if (handlerRegistrations != null) {
			for (ServiceRegistration<EventHandler> r : handlerRegistrations) {
				r.unregister();
			}
		}

		super.dispose();
	}

	class ContentProviderImpl implements ITreeContentProvider {
		
		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<?>)inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if( parentElement instanceof App ) {
				List<Proc> l = appProcesses.get(((App)parentElement).getId());
				if( l != null ) {
					return l.toArray();
				}
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			//TODO We could implement this but it is not required
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof App;
		}
		
	}
	
	static class ElementComparerImpl implements IElementComparer {

		@Override
		public boolean equals(Object a, Object b) {
			if( a instanceof Proc && b instanceof Proc ) {
				return hashCode(a) == hashCode(b);
			} else if( a instanceof App && b instanceof App ) {
				return hashCode(a) == hashCode(b);
			}
			return a.equals(b);
		}

		@Override
		public int hashCode(Object element) {
			if( element instanceof App ) {
				return ((App) element).getId().hashCode();
			} else if( element instanceof Proc ) {
				return ((Proc) element).getUpid().hashCode();
			}
			return element.hashCode();
		}
	}
	
	
}
