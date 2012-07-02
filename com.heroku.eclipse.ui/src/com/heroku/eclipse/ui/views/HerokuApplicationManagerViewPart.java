package com.heroku.eclipse.ui.views;

import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuProperties;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuServices.APP_FIELDS;
import com.heroku.eclipse.core.services.HerokuServices.IMPORT_TYPES;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.HerokuProc;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.Messages;
import com.heroku.eclipse.ui.git.HerokuCredentialsProvider;
import com.heroku.eclipse.ui.utils.AppComparator;
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

	private static HerokuServices herokuService;

	private List<ServiceRegistration<EventHandler>> handlerRegistrations;

	private Map<String, List<HerokuProc>> appProcesses = new HashMap<String, List<HerokuProc>>();
	private Map<String, String> procApps = new HashMap<String, String>();

	private Timer refreshTimer = new Timer(true);

	private TimerTask refreshTask;

	private TreeViewerColumn urlColumn;

	private TreeViewerColumn gitColumn;

	private TreeViewerColumn nameColumn;

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
		viewer.setComparator(new AppComparator());

		{
			nameColumn = new TreeViewerColumn(viewer, SWT.NONE);
			nameColumn.setLabelProvider(LabelProviderFactory.createName(herokuService, new RunnableWithReturn<List<HerokuProc>, App>() {
				@Override
				public List<HerokuProc> run(App argument) {
					return appProcesses.get(argument.getId());
				}
			}));

			TreeColumn col = nameColumn.getColumn();
			col.setText(Messages.getString("HerokuAppManagerViewPart_Name")); //$NON-NLS-1$
			col.setWidth(200);
			col.addSelectionListener(getSelectionAdapter(col));
			col.setData(AppComparator.SORT_IDENTIFIER, APP_FIELDS.APP_NAME);
		}

		{
			gitColumn = new TreeViewerColumn(viewer, SWT.NONE);
			gitColumn.setLabelProvider(LabelProviderFactory.createApp_GitUrl());

			TreeColumn col = gitColumn.getColumn();
			col.setText(Messages.getString("HerokuAppManagerViewPart_GitUrl")); //$NON-NLS-1$
			col.setWidth(200);
			col.setData(AppComparator.SORT_IDENTIFIER, APP_FIELDS.APP_GIT_URL);
			col.addSelectionListener(getSelectionAdapter(col));
		}

		{
			urlColumn = new TreeViewerColumn(viewer, SWT.NONE);
			urlColumn.setLabelProvider(LabelProviderFactory.createApp_Url());

			TreeColumn col = urlColumn.getColumn();
			col.setText(Messages.getString("HerokuAppManagerViewPart_AppUrl")); //$NON-NLS-1$
			col.setWidth(200);
			col.setData(AppComparator.SORT_IDENTIFIER, APP_FIELDS.APP_WEB_URL);
			col.addSelectionListener(getSelectionAdapter(col));
		}

		{
			MenuManager mgr = createContextMenu();
			Menu menu = mgr.createContextMenu(parent);
			viewer.getControl().setMenu(menu);
		}

		viewer.addOpenListener(new IOpenListener() {

			@Override
			public void open(OpenEvent event) {
				try {
					App app = getSelectedAppOrProcApp();
					if (app != null) {
						openEditor(app);
					}
				}
				catch (HerokuServiceException e1) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to display app info", e1); //$NON-NLS-1$
					e1.printStackTrace();
					HerokuUtils.herokuError(getShell(), e1);
				}
			}

		});

		// sorting by name per default
		viewer.getTree().setSortColumn(nameColumn.getColumn());
		viewer.getTree().setSortDirection(SWT.UP);
		viewer.refresh();

		refreshApplications();
		subscribeToEvents();
	}

	private void openEditor(App app) {
		try {
			getSite().getWorkbenchWindow().getActivePage().openEditor(new ApplicationEditorInput(app), ApplicationInfoEditor.ID, true);
		}
		catch (PartInitException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to open editor for app " + app.getName(), e); //$NON-NLS-1$
			e.printStackTrace();
			HerokuUtils.internalError(getShell(), e);
		}

	}

	private SelectionAdapter getSelectionAdapter(final TreeColumn column) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				int dir = viewer.getTree().getSortDirection();
				if (viewer.getTree().getSortColumn() == column) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				}
				else {
					dir = SWT.DOWN;
				}

				viewer.getTree().setSortColumn(column);
				viewer.getTree().setSortDirection(dir);
				viewer.refresh();
			}
		};
		return selectionAdapter;
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

	HerokuProc getSelectedProc() {
		IStructuredSelection s = (IStructuredSelection) viewer.getSelection();

		return !s.isEmpty() && s.getFirstElement() instanceof HerokuProc ? (HerokuProc) s.getFirstElement() : null;
	}

	App getSelectedAppOrProcApp() throws HerokuServiceException {
		App app = getSelectedApp();

		if (app == null) {
			HerokuProc proc = getSelectedProc();
			if (proc != null) {
				app = herokuService.getApp(proc.getHerokuProc().getAppName());
			}
		}

		return app;
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

		final Action appInfo = new Action(Messages.getString("HerokuAppManagerViewPart_AppInfoShort")) { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					App app = getSelectedAppOrProcApp();
					if (app != null) {
						openEditor(app);
					}
				}
				catch (HerokuServiceException e) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to display app info", e); //$NON-NLS-1$
					e.printStackTrace();
					HerokuUtils.internalError(getShell(), e);
				}

			}
		};
		final Action importApp = new Action(Messages.getString("HerokuAppManagerViewPart_Import")) { //$NON-NLS-1$
			@Override
			public void run() {
				App app = getSelectedApp();
				if (app != null) {
					if (MessageDialog
							.openQuestion(
									getShell(),
									Messages.getString("HerokuAppManagerViewPart_Import"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_Import", app.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							importApp(app);
						}
						catch (HerokuServiceException e) {
							Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to restart app " + app.getName(), e); //$NON-NLS-1$
							e.printStackTrace();
							HerokuUtils.internalError(getShell(), e);
						}
					}
				}
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
				final App app = getSelectedApp();
				if (app != null) {
					if (MessageDialog
							.openQuestion(
									getShell(),
									Messages.getString("HerokuAppManagerViewPart_Restart"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_Restart", app.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_RestartingApp", app.getName()), 2); //$NON-NLS-1$
									monitor.worked(1);
									try {
										herokuService.restartApplication(app);
										monitor.worked(1);
										monitor.done();
									}
									catch (HerokuServiceException e) {
										// rethrow to outer space
										throw new InvocationTargetException(e);
									}
								}
							});
							refreshApplications();

						}
						catch (InvocationTargetException e) {
							Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to restart app " + app.getName(), e); //$NON-NLS-1$
							e.printStackTrace();
							HerokuUtils.internalError(getShell(), e);
						}
						catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else {
					HerokuProc proc = getSelectedProc();
					if (proc != null) {
						if (MessageDialog
								.openQuestion(
										getShell(),
										Messages.getString("HerokuAppManagerViewPart_Restart"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_RestartProc", proc.getDynoName()))) { //$NON-NLS-1$ //$NON-NLS-2$
							try {
								// create process list for the given dyno
								List<HerokuProc> allProcs = appProcesses.get(procApps.get(proc.getUniqueId()));
								final String dynoName = proc.getDynoName();
								
								final List<HerokuProc> dynoProcs = new ArrayList<HerokuProc>();
								for (HerokuProc herokuProc : allProcs) {
									if ( herokuProc.getDynoName().equals(dynoName)) {
										dynoProcs.add(herokuProc);
									}
								}
								PlatformUI.getWorkbench().getProgressService().run(false, true, new IRunnableWithProgress() {
									@Override
									public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
										monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_RestartingProc", dynoName), 2); //$NON-NLS-1$
										monitor.worked(1);
										try {
											herokuService.restartProcs(dynoProcs);
											monitor.worked(1);
											monitor.done();
										}
										catch (HerokuServiceException e) {
											// rethrow to outer space
											throw new InvocationTargetException(e);
										}
									}
								});
								refreshApplications();

							}
							catch (InvocationTargetException e) {
								Activator.getDefault().getLogger()
								.log(LogService.LOG_ERROR, "unknown error when trying to restart all '" + proc.getDynoName()+"' processes", e); //$NON-NLS-1$ //$NON-NLS-2$
								e.printStackTrace();
								HerokuUtils.internalError(getShell(), e);
							}
							catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}
		};

		final Action viewLogs = new Action(Messages.getString("HerokuAppManagerViewPart_ViewLogs")) { //$NON-NLS-1$
			@Override
			public void run() {
				final App app = getSelectedApp();
				if (app != null) {

					try {
						ConsoleViewPart console = (ConsoleViewPart) getSite().getWorkbenchWindow().getActivePage().showView(ConsoleViewPart.ID);
						console.openLog(app);
					}
					catch (PartInitException e) {
						Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to display log for app " + app.getName(), e); //$NON-NLS-1$
						e.printStackTrace();
						HerokuUtils.internalError(getShell(), e);
					}
				}
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
					if (MessageDialog
							.openQuestion(
									getShell(),
									Messages.getString("HerokuAppManagerViewPart_Destroy"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_Destroy", app.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
						try {
							herokuService.destroyApplication(app);
						}
						catch (HerokuServiceException e) {
							if (e.getErrorCode() == HerokuServiceException.NOT_ALLOWED) {
								HerokuUtils
										.userError(
												getShell(),
												Messages.getString("HerokuAppManagerViewPart_Error_DestroyOwner_Title"), Messages.getFormattedString("HerokuAppManagerViewPart_Error_DestroyOwner", app.getName(), app.getOwnerEmail())); //$NON-NLS-1$ //$NON-NLS-2$
							}
							else {
								Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to destroy app " + app.getName(), e); //$NON-NLS-1$
								e.printStackTrace();
								HerokuUtils.herokuError(getShell(), e);
							}
						}
					}
				}
			}
		};

		MenuManager mgr = new MenuManager();
		mgr.add(refresh);
		mgr.add(new Separator());
		mgr.add(appInfo);
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

				// owner restricted actions
				scale.setEnabled(false);
				destroy.setEnabled(false);
				if (enabled) {
					if (s.getFirstElement() instanceof App) {
						App app = (App) s.getFirstElement();
						try {
							if (herokuService.isOwnApp(app)) {
								scale.setEnabled(true);
								destroy.setEnabled(true);
							}
						}
						catch (HerokuServiceException e) {
							Activator.getDefault().getLogger()
									.log(LogService.LOG_ERROR, "unknown error when trying to determine if app " + app.getName() + " is owned by myself", e); //$NON-NLS-1$ //$NON-NLS-2$
							HerokuUtils.herokuError(getShell(), e);
						}
					}
					else if (s.getFirstElement() instanceof HerokuProc) {
						HerokuProc proc = (HerokuProc) s.getFirstElement();
						importApp.setEnabled(false);
						open.setEnabled(false);
						try {
							App app = herokuService.getApp(proc.getHerokuProc().getAppName());
							if (herokuService.isOwnApp(app)) {
								scale.setEnabled(true);
							}
						}
						catch (HerokuServiceException e) {
							Activator.getDefault().getLogger()
									.log(LogService.LOG_ERROR, "unknown error when trying to determine if app " + proc.getHerokuProc().getAppName() + " is owned by myself", e); //$NON-NLS-1$ //$NON-NLS-2$
							HerokuUtils.herokuError(getShell(), e);
						}

					}
				}
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
				}
				catch (Throwable e) {
					e.printStackTrace();
					HerokuUtils.internalError(Display.getCurrent().getActiveShell(), e);
				}

				return Status.OK_STATUS;
			}
		};
		o.schedule();
	}

	private void saveRefreshApplications() {
		try {
			appProcesses.clear();
			procApps.clear();
			if (herokuService.isReady()) {
				List<App> applications = herokuService.listApps();
				for (App a : applications) {
					List<HerokuProc> procs = herokuService.listProcesses(a);
					appProcesses.put(a.getId(), procs);
					if (procs.size() > 0) {
						if (!procApps.containsKey(procs.get(0).getUniqueId())) {
							procApps.put(procs.get(0).getUniqueId(), a.getId());
						}
					}
				}
				HerokuUtils.runOnDisplay(true, viewer, applications, ViewerOperations.input(viewer));
			}
			else {
				HerokuUtils.runOnDisplay(true, viewer, new Object[0], ViewerOperations.input(viewer));
			}

			if (refreshTask != null) {
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
		procApps.clear();
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
			return ((List<?>) inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof App) {
				List<HerokuProc> l = appProcesses.get(((App) parentElement).getId());
				if (l != null) {
					return l.toArray();
				}
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			// TODO We could implement this but it is not required
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
			if (a instanceof HerokuProc && b instanceof HerokuProc) {
				return hashCode(a) == hashCode(b);
			}
			else if (a instanceof App && b instanceof App) {
				return hashCode(a) == hashCode(b);
			}
			return a.equals(b);
		}

		@Override
		public int hashCode(Object element) {
			if (element instanceof App) {
				return ((App) element).getId().hashCode();
			}
			else if (element instanceof HerokuProc) {
				return ((HerokuProc) element).getUniqueId().hashCode();
			}
			return element.hashCode();
		}
	}

	private void importApp(final App app) throws HerokuServiceException {
		if (app != null) {
			final String destinationDir = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
			final int timeout = org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore().getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			final HerokuCredentialsProvider cred = new HerokuCredentialsProvider(HerokuProperties.getString("heroku.eclipse.git.defaultUser"), ""); //$NON-NLS-1$ //$NON-NLS-2$
			// TODO: display import type wizard (autodetect, general, new
			// project wizard)
			try {
				herokuService.materializeGitApp(app, IMPORT_TYPES.AUTODETECT, null, destinationDir, timeout,
						Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), cred, new NullProgressMonitor()); //$NON-NLS-1$
			}
			catch (HerokuServiceException e) {
				if (e.getErrorCode() == HerokuServiceException.INVALID_LOCAL_GIT_LOCATION) {
					HerokuUtils
							.userError(
									getShell(),
									Messages.getString("HerokuAppCreateNamePage_Error_GitLocationInvalid_Title"), Messages.getFormattedString("HerokuAppCreateNamePage_Error_GitLocationInvalid", destinationDir + System.getProperty("file.separator") + app.getName())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				else {
					e.printStackTrace();
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
					HerokuUtils.internalError(getShell(), e);
				}
			}
		}

	}
}
