package com.heroku.eclipse.ui.views;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
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
import com.heroku.eclipse.ui.git.HerokuCredentialsProvider;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.AppComparator;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.IconKeys;
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

	private Map<String, List<HerokuProc>> appProcesses = Collections.synchronizedMap(new HashMap<String, List<HerokuProc>>());
	private Map<String, String> procApps = Collections.synchronizedMap(new HashMap<String, String>());

	private Map<String, Thread> logThreads = new HashMap<String, Thread>();

	private Timer refreshTimer = new Timer(true);
	private TimerTask refreshTask;

	private TreeViewerColumn urlColumn;
	private TreeViewerColumn gitColumn;
	private TreeViewerColumn nameColumn;

	private Action refreshAction;

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
					HerokuUtils.herokuError(getShell(), e1);
				}
			}

		});

		// sorting by name per default
		viewer.getTree().setSortColumn(nameColumn.getColumn());
		viewer.getTree().setSortDirection(SWT.UP);
		viewer.refresh();

		createToolbar();

		// register our action to global refresher
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

		refreshApplications(new NullProgressMonitor(), false);
		subscribeToEvents();
	}

	private void createToolbar() {
		refreshAction = new Action(null, IconKeys.getImageDescriptor(IconKeys.ICON_APPSLIST_REFRESH)) {
			public void run() {
				refreshApplications(new NullProgressMonitor(), true);
			}
		};
		refreshAction.setToolTipText(Messages.getString("HerokuAppManagerViewPart_Refresh_Tooltip")); //$NON-NLS-1$
		refreshAction.setEnabled(true);

		// Create the local tool bar
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(new Separator(Activator.PLUGIN_ID));
		tbm.appendToGroup(Activator.PLUGIN_ID, refreshAction);
		tbm.update(false);
	}

	private void openEditor(App app) {
		try {
			getSite().getWorkbenchWindow().getActivePage().openEditor(new ApplicationEditorInput(app), ApplicationInfoEditor.ID, true);
		}
		catch (PartInitException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to open editor for app " + app.getName(), e); //$NON-NLS-1$
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
				refreshApplications(new NullProgressMonitor(), true);
			}
		};
		refreshTimer.schedule(refreshTask, Integer.parseInt(HerokuProperties.getString("heroku.eclipse.appsList.refreshInterval"))); //$NON-NLS-1$
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
				app = herokuService.getApp(new NullProgressMonitor(), proc.getHerokuProc().getAppName());
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
				refreshApplications(new NullProgressMonitor(), true);
			}
		};

		final Action appInfo = new Action(
				Messages.getString("HerokuAppManagerViewPart_AppInfoShort"), IconKeys.getImageDescriptor(IconKeys.ICON_APPINFO_EDITOR_ICON)) { //$NON-NLS-1$
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
							Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to import app " + app.getName(), e); //$NON-NLS-1$
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
							PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_RestartingApp", app.getName()), 2); //$NON-NLS-1$
									monitor.worked(1);
									try {
										herokuService.restartApplication(monitor, app);
										monitor.worked(1);
										monitor.done();
									}
									catch (HerokuServiceException e) {
										// rethrow to outer space
										throw new InvocationTargetException(e);
									}
								}
							});
							refreshApplications(new NullProgressMonitor(), true);

						}
						catch (InvocationTargetException e) {
							HerokuServiceException se = extractHerokuException(e, "unknown error when trying to restart app " + app.getName()); //$NON-NLS-1$
							if (se != null) {
								Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to restart app " + app.getName(), e); //$NON-NLS-1$
								HerokuUtils.herokuError(getShell(), e);
							}
						}
						catch (InterruptedException e) {
							Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to restart app " + app.getName(), e); //$NON-NLS-1$
							HerokuUtils.internalError(getShell(), e);
						}
					}
				}
				else {
					final HerokuProc proc = getSelectedProc();
					if (proc != null) {
						if (MessageDialog
								.openQuestion(
										getShell(),
										Messages.getString("HerokuAppManagerViewPart_Restart"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_RestartProc", proc.getDynoName()))) { //$NON-NLS-1$ //$NON-NLS-2$
							try {
								PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
									@Override
									public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
										monitor.beginTask(
												Messages.getFormattedString("HerokuAppManagerViewPart_Progress_RestartingProc", proc.getDynoName()), 2); //$NON-NLS-1$
										monitor.worked(1);
										try {
											herokuService.restartDyno(monitor, proc);
											monitor.worked(1);
											monitor.done();
										}
										catch (HerokuServiceException e) {
											// rethrow to outer space
											throw new InvocationTargetException(e);
										}
									}
								});
								refreshApplications(new NullProgressMonitor(), true);

							}
							catch (InvocationTargetException e) {
								HerokuServiceException se = extractHerokuException(e,
										"unknown error when trying to restart all '" + proc.getDynoName() + "' processes"); //$NON-NLS-1$ //$NON-NLS-2$
								if (se != null) {
									Activator.getDefault().getLogger()
											.log(LogService.LOG_ERROR, "unknown error when trying to restart all '" + proc.getDynoName() + "' processes", e); //$NON-NLS-1$ //$NON-NLS-2$
									HerokuUtils.herokuError(getShell(), e);
								}
							}
							catch (InterruptedException e) {
								Activator.getDefault().getLogger()
										.log(LogService.LOG_ERROR, "unknown error when trying to restart all '" + proc.getDynoName() + "' processes", e); //$NON-NLS-1$ //$NON-NLS-2$
								e.printStackTrace();
								HerokuUtils.internalError(getShell(), e);
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
					openLog(app.getId(), app.getName(), null);
				}
				else {
					// opening log for the entire process group
					HerokuProc proc = getSelectedProc();
					openLog(proc.getUniqueId(), proc.getHerokuProc().getAppName(), proc.getDynoName());
				}
			}
		};

		final Action scale = new Action(Messages.getString("HerokuAppManagerViewPart_Scale")) { //$NON-NLS-1$
			@Override
			public void run() {
				TrayDialog d = new TrayDialog(getShell()) {

					private Text processField;
					private Spinner quantityField;
					private String appName;

					@Override
					protected Control createDialogArea(Composite parent) {
						int quantity = 0;
						String dynoName = ""; //$NON-NLS-1$

						App app = getSelectedApp();
						if (app != null) {
							List<HerokuProc> procs = appProcesses.get(app.getId());
							appName = app.getName();

							// if the app has only one process type, prepopulate
							for (HerokuProc herokuProc : procs) {
								if (dynoName.equals("")) { //$NON-NLS-1$
									dynoName = herokuProc.getDynoName();
									quantity++;
								}
								else if (!herokuProc.getDynoName().equals(dynoName)) {
									dynoName = ""; //$NON-NLS-1$
									quantity = 0;
									break;
								}
							}
						}
						else {
							HerokuProc proc = getSelectedProc();
							dynoName = proc.getDynoName();
							quantity = findDynoProcs(proc).size();
							appName = proc.getHerokuProc().getAppName();
						}

						Composite container = (Composite) super.createDialogArea(parent);
						getShell().setText(Messages.getString("HerokuAppManagerViewPart_Scale_Title")); //$NON-NLS-1$

						Composite area = new Composite(container, SWT.NONE);
						area.setLayout(new GridLayout(2, false));
						area.setLayoutData(new GridData(GridData.FILL_BOTH));

						{
							Label l = new Label(area, SWT.NONE);
							l.setText(Messages.getString("HerokuAppManagerViewPart_Scale_Process")); //$NON-NLS-1$

							processField = new Text(area, SWT.BORDER);
							processField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
							processField.setText(dynoName);
						}

						{
							Label l = new Label(area, SWT.NONE);
							l.setText(Messages.getString("HerokuAppManagerViewPart_Scale_ScaleTo")); //$NON-NLS-1$

							quantityField = new Spinner(area, SWT.BORDER);
							quantityField.setMinimum(0);
							quantityField.setMaximum(Integer.parseInt(HerokuProperties.getString("heroku.eclipse.dynos.maxQuantity"))); //$NON-NLS-1$
							quantityField.setSelection(quantity);
							quantityField.setIncrement(1);
							quantityField.pack();
						}

						return container;
					}

					@Override
					protected void okPressed() {
						final String process = processField.getText().trim();
						final String quantity = quantityField.getText();
						if (HerokuUtils.isNotEmpty(process)) {
							try {
								PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
									@Override
									public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
										monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_Scaling", process), 3); //$NON-NLS-1$
										monitor.worked(1);
										try {
											herokuService.scaleProcess(monitor, appName, process, Integer.parseInt(quantity));
											monitor.worked(1);
											refreshApplications(monitor, true);
											monitor.done();
										}
										catch (HerokuServiceException e) {
											// rethrow to outer space
											throw new InvocationTargetException(e);
										}
									}
								});
								super.okPressed();
							}
							catch (InvocationTargetException e) {
								if ((e.getCause() instanceof HerokuServiceException)) {
									HerokuServiceException e1 = (HerokuServiceException) e.getCause();

									if (e1.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
										HerokuUtils
												.userError(
														getShell(),
														Messages.getString("HerokuAppManagerViewPart_Scale_Error_BuyCredits_Title"), Messages.getString("HerokuAppManagerViewPart_Scale_Error_BuyCredits")); //$NON-NLS-1$ //$NON-NLS-2$
									}
									else if (e1.getErrorCode() == HerokuServiceException.NOT_FOUND) {
										HerokuUtils
												.userError(
														getShell(),
														Messages.getString("HerokuAppManagerViewPart_Scale_Error_UnknownDyno_Title"), Messages.getFormattedString("HerokuAppManagerViewPart_Scale_Error_UnknownDyno", process)); //$NON-NLS-1$ //$NON-NLS-2$
									}
									else {
										HerokuUtils.herokuError(getShell(), e);
									}
								}
								else {
									Activator.getDefault().getLogger()
											.log(LogService.LOG_ERROR, "unknown error when trying to scale process " + process + " for app " + appName, e); //$NON-NLS-1$ //$NON-NLS-2$
									HerokuUtils.internalError(getShell(), e);
								}
							}
							catch (InterruptedException e) {
								Activator.getDefault().getLogger()
										.log(LogService.LOG_ERROR, "unknown error when trying to scale process " + process + " for app " + appName, e); //$NON-NLS-1$ //$NON-NLS-2$
								HerokuUtils.internalError(getShell(), e);
							}
						}
						else {
							HerokuUtils
									.userError(
											getShell(),
											Messages.getString("HerokuAppManagerViewPart_Scale_Error_MissingInput_Title"), Messages.getString("HerokuAppManagerViewPart_Scale_Error_MissingInput")); //$NON-NLS-1$ //$NON-NLS-2$
							processField.setFocus();
						}
					}
				};
				d.open();
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
							herokuService.destroyApplication(new NullProgressMonitor(), app);
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
				NullProgressMonitor pm = new NullProgressMonitor();
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
							if (herokuService.isOwnApp(pm, app)) {
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
							App app = herokuService.getApp(pm, proc.getHerokuProc().getAppName());
							if (herokuService.isOwnApp(pm, app)) {
								scale.setEnabled(true);
							}
						}
						catch (HerokuServiceException e) {
							Activator
									.getDefault()
									.getLogger()
									.log(LogService.LOG_ERROR,
											"unknown error when trying to determine if app " + proc.getHerokuProc().getAppName() + " is owned by myself", e); //$NON-NLS-1$ //$NON-NLS-2$
							HerokuUtils.herokuError(getShell(), e);
						}

					}
				}
			}
		});

		return mgr;
	}

	private void openLog(final String uniqueId, final String appName, final String procName) {
		String consoleName = ""; //$NON-NLS-1$
		String streamName = "logstream-"; //$NON-NLS-1$

		if (procName == null) {
			consoleName = Messages.getFormattedString("HerokuAppManagerViewPart_AppConsole_Title", appName); //$NON-NLS-1$
			streamName += "app-" + uniqueId; //$NON-NLS-1$
		}
		else {
			consoleName += Messages.getFormattedString("HerokuAppManagerViewPart_ProcConsole_Title", appName, procName); //$NON-NLS-1$
			streamName += "proc-" + uniqueId; //$NON-NLS-1$
		}

		MessageConsole console = HerokuUtils.findConsole(consoleName);
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
		console.activate();

		// only open new log stream if we have not opened it before
		if (!logThreads.containsKey(streamName)) {
			final MessageConsoleStream out = console.newMessageStream();
			Thread t = new Thread(streamName) {
				AtomicBoolean wantsFun = new AtomicBoolean(true);

				@Override
				public void run() {
					while (wantsFun.get()) {
						byte[] buffer = new byte[1024];
						int bytesRead;
						try {
							InputStream is;
							if (procName == null) {
								is = herokuService.getApplicationLogStream(new NullProgressMonitor(), appName);
							}
							else {
								is = herokuService.getProcessLogStream(new NullProgressMonitor(), appName, procName);
							}
							while ((bytesRead = is.read(buffer)) != -1) {
								if (out.isClosed()) {
									break;
								}
								out.write(buffer, 0, bytesRead);
							}
						}
						catch (IOException e) {
							HerokuUtils.internalError(getShell(), e);
						}
						catch (HerokuServiceException e) {
							HerokuUtils.herokuError(getShell(), e);
						}
					}
				}

				@Override
				public void interrupt() {
					wantsFun.set(false);
					try {
						out.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					super.interrupt();
				}
			};

			t.setDaemon(true);
			t.start();

			logThreads.put(streamName, t);
		}

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
			HerokuUtils.internalError(getShell(), e);
		}
		catch (MalformedURLException e) {
			HerokuUtils.internalError(getShell(), e);
		}
	}

	private void subscribeToEvents() {

		EventHandler sessionInvalidationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications(new NullProgressMonitor(), true);
			}
		};

		EventHandler newApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications(new NullProgressMonitor(), true);
			}
		};

		EventHandler renameApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications(new NullProgressMonitor(), true);
			}
		};

		EventHandler transferApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications(new NullProgressMonitor(), true);
			}
		};

		EventHandler destroyedApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				refreshApplications(new NullProgressMonitor(), true);
			}
		};

		handlerRegistrations = new ArrayList<ServiceRegistration<EventHandler>>();
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(sessionInvalidationHandler, HerokuServices.TOPIC_SESSION_INVALID));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(newApplicationHandler, HerokuServices.TOPIC_APPLICATION_NEW));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(renameApplicationHandler, HerokuServices.TOPIC_APPLICATION_RENAMED));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(transferApplicationHandler, HerokuServices.TOPIC_APPLICATION_TRANSFERED));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(destroyedApplicationHandler, HerokuServices.TOPIC_APPLICATION_DESTROYED));
	}

	private void refreshApplications(final IProgressMonitor pm, final boolean refreshProcs) {
		try {
			if (herokuService.isReady(pm)) {
				final Job o = new Job(Messages.getString("HerokuAppManagerViewPart_RefreshApps")) { //$NON-NLS-1$

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							if (saveRefreshApplications(pm, refreshProcs)) {
								// 2nd run: refresh procs now as well
								saveRefreshApplications(pm, true);
							}
						}
						catch (HerokuServiceException e) {
							if (Display.getCurrent() != null) {
								HerokuUtils.herokuError(Display.getCurrent().getActiveShell(), e);
							}
							else {
								e.printStackTrace();
							}
							return Status.CANCEL_STATUS;
						}
						catch (Throwable e) {
							if (Display.getCurrent() != null) {
								HerokuUtils.internalError(Display.getCurrent().getActiveShell(), e);
							}
							else {
								e.printStackTrace();
							}
							return Status.CANCEL_STATUS;
						}

						return Status.OK_STATUS;
					}
				};
				o.schedule();
			}
		}
		catch (HerokuServiceException e) {
			HerokuUtils.herokuError(getShell(), e);
		}
	}

	private boolean saveRefreshApplications(IProgressMonitor pm, boolean refreshProcs) throws HerokuServiceException {
		boolean rv = true;

		appProcesses.clear();
		procApps.clear();
		if (herokuService.isReady(pm)) {
			List<App> applications = herokuService.listApps(pm);
			if (refreshProcs) { // || applications.size() < 10) {
				for (App a : applications) {
					List<HerokuProc> procs = herokuService.listProcesses(pm, a);
					appProcesses.put(a.getId(), procs);
					if (procs.size() > 0) {
						if (!procApps.containsKey(procs.get(0).getUniqueId())) {
							procApps.put(procs.get(0).getUniqueId(), a.getId());
						}
					}
				}
				rv = false;
			}
			HerokuUtils.runOnDisplay(true, viewer, applications, ViewerOperations.input(viewer));
		}
		else {
			HerokuUtils.runOnDisplay(true, viewer, new Object[0], ViewerOperations.input(viewer));
		}

		if (refreshTask != null) {
			refreshTask.cancel();
		}

		return rv;

		// scheduleRefresh();
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		appProcesses.clear();
		procApps.clear();
		refreshTimer.cancel();

		if (logThreads != null) {
			for (Thread t : logThreads.values()) {
				t.interrupt();
			}
			logThreads.clear();
		}

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
			return getChildren(element).length > 0;
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
			try {
				herokuService.materializeGitApp(new NullProgressMonitor(), app, IMPORT_TYPES.AUTODETECT, null, destinationDir, timeout,
						Messages.getFormattedString("HerokuAppCreate_CreatingApp", app.getName()), cred); //$NON-NLS-1$
			}
			catch (HerokuServiceException e) {
				if (e.getErrorCode() == HerokuServiceException.INVALID_LOCAL_GIT_LOCATION) {
					HerokuUtils
							.userError(
									getShell(),
									Messages.getString("HerokuAppCreateNamePage_Error_GitLocationInvalid_Title"), Messages.getFormattedString("HerokuAppCreateNamePage_Error_GitLocationInvalid", destinationDir + System.getProperty("file.separator") + app.getName())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
				else {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "internal error during git checkout, aborting ...", e); //$NON-NLS-1$
					HerokuUtils.internalError(getShell(), e);
				}
			}
		}

	}

	private List<HerokuProc> findDynoProcs(HerokuProc proc) {
		// create process list for the given dyno
		List<HerokuProc> allProcs = appProcesses.get(procApps.get(proc.getUniqueId()));
		final String dynoName = proc.getDynoName();

		final List<HerokuProc> dynoProcs = new ArrayList<HerokuProc>();
		for (HerokuProc herokuProc : allProcs) {
			if (herokuProc.getDynoName().equals(dynoName)) {
				dynoProcs.add(herokuProc);
			}
		}

		return dynoProcs;
	}

	private HerokuServiceException extractHerokuException(Exception e, String defaultError) {
		Exception rv = e;
		if (e instanceof InvocationTargetException) {
			rv = (Exception) e.getCause();
		}

		if (rv instanceof HerokuServiceException) {
			// "Operation cancelled" means that the user pressed "cancel", so we
			// ignore that in terms of the thrown exception and return null
			if (((HerokuServiceException) rv).getErrorCode() != HerokuServiceException.OPERATION_CANCELLED) {
				return (HerokuServiceException) rv;
			}
		}
		else {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, defaultError, e);
			HerokuUtils.internalError(getShell(), e);
		}

		return null;
	}
}
