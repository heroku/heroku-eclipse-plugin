package com.heroku.eclipse.ui.views.dialog;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuProperties;
import com.heroku.eclipse.core.services.HerokuServices.LogStream;
import com.heroku.eclipse.core.services.HerokuServices.LogStreamCreator;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.HerokuProc;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.LabelProviderFactory;
import com.heroku.eclipse.ui.utils.RunnableWithReturn;
import com.heroku.eclipse.ui.utils.ViewerOperations;

/**
 * Displays all processes for an Heroku App
 * 
 * @author udo.rader@bestsolution.at
 */
public class ProcessListingPart {

	private App domainObject;
	private List<HerokuProc> processList;
	private Composite parent;
	private TableViewer viewer;
	private Button refreshButton;
	private Button logsButton;
	private Button scaleButton;
	private Button restartButton;

	/**
	 * Creates the UI
	 * 
	 * @param parent
	 * @return the container with the UI stuff inside
	 */
	public Composite createUI(Composite parent) {
		this.parent = parent;
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		{
			viewer = new TableViewer(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
			viewer.getTable().setHeaderVisible(true);
			viewer.getTable().setLinesVisible(true);
			viewer.setContentProvider(new ArrayContentProvider());

			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.heightHint = 300;
			viewer.getControl().setLayoutData(gd);
			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText(Messages.getString("HerokuAppInformationProcesses_Status")); //$NON-NLS-1$
				column.getColumn().setWidth(60);
				column.setLabelProvider(LabelProviderFactory.createProcess_state());
			}

			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText(Messages.getString("HerokuAppInformationProcesses_ProcessType")); //$NON-NLS-1$
				column.getColumn().setWidth(100);
				column.setLabelProvider(LabelProviderFactory.createProcess_type());
			}

			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText(Messages.getString("HerokuAppInformationProcesses_Dynos")); //$NON-NLS-1$
				column.getColumn().setWidth(50);
				column.setLabelProvider(LabelProviderFactory.createProcess_dynoCount(new RunnableWithReturn<List<HerokuProc>, HerokuProc>() {
					@Override
					public List<HerokuProc> run(HerokuProc argument) {
						return findDynoProcs(argument);
					}
				}));
			}

			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText(Messages.getString("HerokuAppInformationProcesses_Command")); //$NON-NLS-1$
				column.getColumn().setWidth(200);
				column.setLabelProvider(LabelProviderFactory.createProcess_Command());
			}

			viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

			viewer.addSelectionChangedListener(new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					scaleButton.setEnabled(true);
					logsButton.setEnabled(true);
					if ((((IStructuredSelection) viewer.getSelection()).toList()).size() > 1) {
						scaleButton.setEnabled(false);
					}
				}
			});

		}

		{
			Composite controls = new Composite(container, SWT.NONE);
			controls.setLayout(new GridLayout(1, true));

			{
				scaleButton = new Button(controls, SWT.PUSH);
				scaleButton.setText(Messages.getString("HerokuAppInformationProcesses_Scale")); //$NON-NLS-1$
				scaleButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				scaleButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						@SuppressWarnings("unchecked")
						List<HerokuProc> procs = ((IStructuredSelection) viewer.getSelection()).toList();
						// only one proc selected -> prepopulate popup
						if (procs.size() == 1) {
							handleScale(scaleButton.getShell(), procs.get(0));
						}
						else {
							handleScale(scaleButton.getShell(), null);
						}
					}
				});
				scaleButton.setEnabled(true);
			}

			{
				logsButton = new Button(controls, SWT.PUSH);
				logsButton.setText(Messages.getString("HerokuAppInformationProcesses_Logs")); //$NON-NLS-1$
				logsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				logsButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						@SuppressWarnings("unchecked")
						List<HerokuProc> procs = ((IStructuredSelection) viewer.getSelection()).toList();
						// only one proc selected -> show logs only for this
						// proc
						if (procs.size() == 1) {
							handleLogs(logsButton.getShell(), procs.get(0));
						}
						// otherwise show app log
						else {
							handleLogs(scaleButton.getShell(), null);
						}
					}
				});
			}

			{
				restartButton = new Button(controls, SWT.PUSH);
				restartButton.setText(Messages.getString("HerokuAppManagerViewPart_Restart")); //$NON-NLS-1$
				restartButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				restartButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						@SuppressWarnings("unchecked")
						List<HerokuProc> procs = ((IStructuredSelection) viewer.getSelection()).toList();
						// only one proc selected -> restart only this process
						if (procs.size() == 1) {
							handleRestart(logsButton.getShell(), procs.get(0));
						}
						// otherwise restart entire app
						else {
							handleRestart(scaleButton.getShell(), null);
						}
					}
				});
			}

			{
				refreshButton = new Button(controls, SWT.PUSH);
				refreshButton.setText(Messages.getString("HerokuAppInformationProcesses_Refresh")); //$NON-NLS-1$
				refreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				refreshButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						refreshProcessList();
					}
				});
			}
		}

		return container;
	}

	void handleScale(final Shell shell, final HerokuProc proc) {
		TrayDialog d = new TrayDialog(shell) {

			private Text processField;
			private Spinner quantityField;

			@Override
			protected Control createDialogArea(Composite parent) {
				int quantity = 0;
				String dynoName = ""; //$NON-NLS-1$

				if (proc != null) {
					dynoName = proc.getDynoName();
					quantity = findDynoProcs(proc).size();
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

				if (!HerokuUtils.isNotEmpty(quantity) || !HerokuUtils.isInteger(quantity)) {
					HerokuUtils
							.userError(
									getShell(),
									Messages.getString("HerokuAppManagerViewPart_Scale_Error_MissingInput_Title"), Messages.getString("HerokuAppManagerViewPart_Scale_Error_QuantintyNaN")); //$NON-NLS-1$ //$NON-NLS-2$
					quantityField.setFocus();
					return;
				}

				if (HerokuUtils.isNotEmpty(process)) {
					try {
						PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_Scaling", process), 3); //$NON-NLS-1$
								monitor.worked(1);
								try {
									Activator.getDefault().getService().scaleProcess(monitor, domainObject.getName(), process, Integer.parseInt(quantity));
									monitor.worked(1);
									monitor.done();
								}
								catch (HerokuServiceException e) {
									// rethrow to outer space
									throw new InvocationTargetException(e);
								}
							}
						});
						refreshProcessList();
						super.okPressed();
					}
					catch (InvocationTargetException e) {
						if ((e.getCause() instanceof HerokuServiceException)) {
							HerokuServiceException e1 = (HerokuServiceException) e.getCause();
							if (e1.getErrorCode() == HerokuServiceException.NOT_ALLOWED) {
								HerokuUtils
										.userError(
												getShell(),
												Messages.getString("HerokuAppManagerViewPart_Scale_Error_ScalingUnauthorized_Title"), Messages.getFormattedString("HerokuAppManagerViewPart_Scale_Error_ScalingUnauthorized", domainObject.getOwnerEmail(), domainObject.getName())); //$NON-NLS-1$ //$NON-NLS-2$
							}
							else if (e1.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
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
							Activator
									.getDefault()
									.getLogger()
									.log(LogService.LOG_ERROR,
											"unknown error when trying to scale process " + process + " for app " + domainObject.getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
							HerokuUtils.internalError(getShell(), e);
						}
					}
					catch (InterruptedException e) {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_ERROR, "unknown error when trying to scale process " + process + " for app " + domainObject.getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
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

	void handleLogs(final Shell shell, HerokuProc proc) {
		String consoleName = ""; //$NON-NLS-1$

		if (proc == null) {
			consoleName = Messages.getFormattedString("HerokuAppManagerViewPart_AppConsole_Title", domainObject.getName()); //$NON-NLS-1$
		}
		else {
			consoleName += Messages.getFormattedString("HerokuAppManagerViewPart_ProcConsole_Title", domainObject.getName(), proc.getDynoName()); //$NON-NLS-1$
		}

		// add and activate the fitting console
		final MessageConsole console = HerokuUtils.findConsole(consoleName);
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
		console.activate();

		final LogStream out = new LogStream() {
			private final MessageConsoleStream out = console.newMessageStream();

			@Override
			public void write(byte[] buffer, int i, int bytesRead) throws IOException {
				out.write(buffer, i, bytesRead);
			}

			@Override
			public boolean isClosed() throws IOException {
				return out.isClosed();
			}

			@Override
			public void close() throws IOException {
				out.close();
			}
		};

		UncaughtExceptionHandler exceptionHandler = new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
				HerokuServiceException e1 = HerokuUtils.extractHerokuException(shell, e, "unexpected error displaying log for app " + domainObject.getName()); //$NON-NLS-1$

				if (e1 != null) {
					HerokuUtils.herokuError(shell, e1);
				}
			}
		};

		// open App log, if we have no process to work with
		if (proc == null) {
			Activator.getDefault().getService().startAppLogThread(new NullProgressMonitor(), domainObject, new LogStreamCreator() {
				@Override
				public LogStream create() {
					return out;
				}
			}, exceptionHandler);
		}
		else {
			Activator.getDefault().getService().startProcessLogThread(new NullProgressMonitor(), proc, new LogStreamCreator() {
				@Override
				public LogStream create() {
					return out;
				}
			}, exceptionHandler);
		}
	}

	void handleRestart(Shell shell, final HerokuProc proc) {
		if (proc == null) {
			if (MessageDialog
					.openQuestion(
							shell,
							Messages.getString("HerokuAppManagerViewPart_Restart"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_Restart", domainObject.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_RestartingApp", domainObject.getName()), 2); //$NON-NLS-1$
							monitor.worked(1);
							try {
								Activator.getDefault().getService().restartApplication(monitor, domainObject);
								monitor.worked(1);
								monitor.done();
							}
							catch (HerokuServiceException e) {
								// rethrow to outer space
								throw new InvocationTargetException(e);
							}
						}
					});
					refreshProcessList();
				}
				catch (InvocationTargetException e) {
					HerokuServiceException se = HerokuUtils.extractHerokuException(shell, e,
							"unknown error when trying to restart app " + domainObject.getName()); //$NON-NLS-1$
					if (se != null) {
						Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to restart app " + domainObject.getName(), e); //$NON-NLS-1$
						HerokuUtils.herokuError(shell, e);
					}
				}
				catch (InterruptedException e) {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to restart app " + domainObject.getName(), e); //$NON-NLS-1$
					HerokuUtils.internalError(shell, e);
				}
			}
		}
		else {
			if (MessageDialog
					.openQuestion(
							shell,
							Messages.getString("HerokuAppManagerViewPart_Restart"), Messages.getFormattedString("HerokuAppManagerViewPart_Question_RestartProc", proc.getDynoName()))) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							monitor.beginTask(Messages.getFormattedString("HerokuAppManagerViewPart_Progress_RestartingProc", proc.getDynoName()),2); //$NON-NLS-1$
							monitor.worked(1);
							try {
								Activator.getDefault().getService().restartDyno(monitor, proc);
								monitor.worked(1);
								monitor.done();
							}
							catch (HerokuServiceException e) {
								// rethrow to outer space
								throw new InvocationTargetException(e);
							}
						}
					});
					refreshProcessList();
				}
				catch (InvocationTargetException e) {
					HerokuServiceException se = HerokuUtils.extractHerokuException(shell, e,
							"unknown error when trying to restart all '" + proc.getDynoName() + "' processes"); //$NON-NLS-1$ //$NON-NLS-2$
					if (se != null) {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_ERROR, "unknown error when trying to restart all '" + proc.getDynoName() + "' processes", e); //$NON-NLS-1$ //$NON-NLS-2$
						HerokuUtils.herokuError(shell, e);
					}
				}
				catch (InterruptedException e) {
					Activator.getDefault().getLogger()
							.log(LogService.LOG_ERROR, "unknown error when trying to restart all '" + proc.getDynoName() + "' processes", e); //$NON-NLS-1$ //$NON-NLS-2$
					e.printStackTrace();
					HerokuUtils.internalError(shell, e);
				}
			}
		}
	}

	/**
	 * @param domainObject
	 */
	public void setDomainObject(App domainObject) {
		this.domainObject = domainObject;
		refreshProcessList();
	}

	private void refreshProcessList() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						processList = Activator.getDefault().getService().listProcesses(monitor, domainObject);
						HerokuUtils.runOnDisplay(true, viewer, processList, ViewerOperations.input(viewer));
					}
					catch (HerokuServiceException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof HerokuServiceException) {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to refresh collaborators list", e); //$NON-NLS-1$
				HerokuUtils.herokuError(parent.getShell(), e);
			}
			else {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to refresh collaborators list", e); //$NON-NLS-1$
				HerokuUtils.internalError(parent.getShell(), e);

			}
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<HerokuProc> findDynoProcs(HerokuProc proc) {
		// create process list for the given dyno
		final String dynoName = proc.getDynoName();

		final List<HerokuProc> dynoProcs = new ArrayList<HerokuProc>();
		for (HerokuProc herokuProc : processList) {
			if (herokuProc.getDynoName().equals(dynoName)) {
				dynoProcs.add(herokuProc);
			}
		}

		return dynoProcs;
	}

	/**
	 * Disposes the view part
	 */
	public void dispose() {

	}

	/**
	 * Focuses on the default object 
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}