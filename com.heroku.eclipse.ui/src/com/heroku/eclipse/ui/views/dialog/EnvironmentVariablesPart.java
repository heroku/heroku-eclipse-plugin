package com.heroku.eclipse.ui.views.dialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.KeyValue;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.LabelProviderFactory;
import com.heroku.eclipse.ui.utils.ViewerOperations;

/**
 * View part responsible for displaying environment variables for Heroku Apps
 * 
 * @author tom.schindl@bestsolution.at
 */
public class EnvironmentVariablesPart {

	private App domainObject;
	private List<KeyValue> envList;
	private Composite parent;
	private TableViewer viewer;
	private Button addButton;
	private Button removeButton;
	private Button editButton;

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
				column.getColumn().setText(Messages.getString("HerokuAppInformationEnvironment_Key")); //$NON-NLS-1$
				column.getColumn().setWidth(200);
				column.setLabelProvider(LabelProviderFactory.createEnv_Key());
			}

			{
				TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
				column.getColumn().setText(Messages.getString("HerokuAppInformationEnvironment_Value")); //$NON-NLS-1$
				column.getColumn().setWidth(200);
				column.setLabelProvider(LabelProviderFactory.createEnv_Value());
			}

			viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

			viewer.addSelectionChangedListener(new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					List<KeyValue> envVars = (((IStructuredSelection) viewer.getSelection()).toList());
					editButton.setEnabled(false);
					removeButton.setEnabled(false);
					if (envVars.size() == 1) {
						editButton.setEnabled(true);
						removeButton.setEnabled(true);
					}
					else if (envVars.size() > 1) {
						removeButton.setEnabled(true);
					}
				}
			});

		}

		{
			Composite controls = new Composite(container, SWT.NONE);
			controls.setLayout(new GridLayout(1, true));

			{
				addButton = new Button(controls, SWT.PUSH);
				addButton.setText("+"); //$NON-NLS-1$
				addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				addButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						handleAdd(addButton.getShell());
					}
				});
			}

			{
				removeButton = new Button(controls, SWT.PUSH);
				removeButton.setText("-"); //$NON-NLS-1$
				removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				removeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						@SuppressWarnings("unchecked")
						List<KeyValue> envVars = ((IStructuredSelection) viewer.getSelection()).toList();
						if (envVars.size() > 0) {
							handleRemove(removeButton.getShell(), envVars);
						}
					}
				});
				removeButton.setEnabled(false);
			}

			{
				editButton = new Button(controls, SWT.PUSH);
				editButton.setText(Messages.getString("HerokuAppInformationEnvironment_Edit")); //$NON-NLS-1$
				editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				editButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						@SuppressWarnings("unchecked")
						List<KeyValue> envVars = ((IStructuredSelection) viewer.getSelection()).toList();
						if (envVars.size() == 1) {
							handleEdit(editButton.getShell(), envVars.get(0));
						}
					}
				});
				editButton.setEnabled(false);
			}
		}

		return container;
	}

	void handleEdit(final Shell shell, final KeyValue envVar) {
		TrayDialog d = new TrayDialog(shell) {

			private Text keyField;
			private Text valueField;

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite container = (Composite) super.createDialogArea(parent);
				getShell().setText(Messages.getString("HerokuAppInformationEnvironment_Edit_Title")); //$NON-NLS-1$

				Composite area = new Composite(container, SWT.NONE);
				area.setLayout(new GridLayout(2, false));
				area.setLayoutData(new GridData(GridData.FILL_BOTH));

				{
					Label l = new Label(area, SWT.NONE);
					l.setText(Messages.getString("HerokuAppInformationEnvironment_Edit_Key")); //$NON-NLS-1$

					keyField = new Text(area, SWT.BORDER | SWT.READ_ONLY);
					keyField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					keyField.setText(envVar.getKey());
					keyField.setEnabled(false);
				}

				{
					Label l = new Label(area, SWT.NONE);
					l.setText(Messages.getString("HerokuAppInformationEnvironment_Edit_Value")); //$NON-NLS-1$

					valueField = new Text(area, SWT.BORDER);
					valueField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
					valueField.setText(envVar.getValue());
				}

				return container;
			}

			@Override
			protected void okPressed() {
				String key = keyField.getText().trim();
				String value = valueField.getText().trim();
				if (HerokuUtils.isNotEmpty(value)) {
					if (doAddEnv(shell, key, value)) {
						super.okPressed();
						refreshEnvVariables();
					}
				}
				// emtpy value = ask if user wants to remove
				else {
					if (MessageDialog
							.openQuestion(
									shell,
									Messages.getString("HerokuAppInformationEnvironment_Remove_Title"), Messages.getFormattedString("HerokuAppInformationEnvironment_Remove_QuestionSingle", envVar.getKey()))) { //$NON-NLS-1$ //$NON-NLS-2$
						Activator.getDefault().getLogger().log(LogService.LOG_INFO, "about to remove env variable " + envVar.getKey() + " via edit"); //$NON-NLS-1$ //$NON-NLS-2$

						ArrayList<KeyValue> removeSingle = new ArrayList<KeyValue>();
						removeSingle.add(envVar);

						if (doRemoveEnv(shell, removeSingle)) {
							Activator.getDefault().getLogger()
									.log(LogService.LOG_INFO, "removal of single env variable " + envVar.getKey() + " via edit complete"); //$NON-NLS-1$ //$NON-NLS-2$
							super.okPressed();
							refreshEnvVariables();
						}
					}
				}
			}
		};

		d.open();
	}

	void handleRemove(Shell shell, List<KeyValue> envList) {
		String message;

		if (envList.size() == 1) {
			message = Messages.getFormattedString("HerokuAppInformationEnvironment_Remove_QuestionSingle", envList.get(0).getKey()); //$NON-NLS-1$
		}
		else {
			String removed = ""; //$NON-NLS-1$
			for (KeyValue entry : envList) {
				removed += "* " + entry.getKey() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			message = Messages.getFormattedString("HerokuAppInformationEnvironment_Remove_QuestionMultiple", removed); //$NON-NLS-1$
		}

		if (MessageDialog.openQuestion(shell, Messages.getString("HerokuAppInformationEnvironment_Remove_Title"), message)) { //$NON-NLS-1$
			Activator.getDefault().getLogger().log(LogService.LOG_INFO, "about to remove " + envList.size() + " environment variables"); //$NON-NLS-1$ //$NON-NLS-2$

			if (doRemoveEnv(shell, envList)) {
				Activator.getDefault().getLogger().log(LogService.LOG_INFO, "removal of " + envList.size() + " environment variables complete"); //$NON-NLS-1$ //$NON-NLS-2$
				refreshEnvVariables();
			}
		}
	}

	void handleAdd(final Shell shell) {
		TrayDialog d = new TrayDialog(shell) {

			private Text keyField;
			private Text valueField;

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite container = (Composite) super.createDialogArea(parent);
				getShell().setText(Messages.getString("HerokuAppInformationEnvironment_Add_Title")); //$NON-NLS-1$

				Composite area = new Composite(container, SWT.NONE);
				area.setLayout(new GridLayout(2, false));
				area.setLayoutData(new GridData(GridData.FILL_BOTH));

				{
					Label l = new Label(area, SWT.NONE);
					l.setText(Messages.getString("HerokuAppInformationEnvironment_Add_Key")); //$NON-NLS-1$

					keyField = new Text(area, SWT.BORDER);
					keyField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				}

				{
					Label l = new Label(area, SWT.NONE);
					l.setText(Messages.getString("HerokuAppInformationEnvironment_Add_Value")); //$NON-NLS-1$

					valueField = new Text(area, SWT.BORDER);
					valueField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				}

				return container;
			}

			@Override
			protected void okPressed() {
				String key = keyField.getText().trim();
				String value = valueField.getText().trim();
				if (HerokuUtils.isNotEmpty(key) && HerokuUtils.isNotEmpty(value)) {
					if (!Activator.getDefault().getService().isEnvNameBasicallyValid(key)) {
						Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "rejecting to add invalid named env variable '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						HerokuUtils
								.userError(
										shell,
										Messages.getString("HerokuAppInformationEnvironment_Error_KeyOrValueInvalid_Title"), Messages.getFormattedString("HerokuAppInformationEnvironment_Error_KeyInvalid", key)); //$NON-NLS-1$ //$NON-NLS-2$
						return;
					}

					for (KeyValue entry : envList) {
						if (entry.getKey().equals(keyField.getText().trim())) {
							Activator.getDefault().getLogger()
									.log(LogService.LOG_DEBUG, "rejecting to add already existing env variable '" + entry.getKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
							HerokuUtils
									.userError(
											shell,
											Messages.getString("HerokuAppInformationEnvironment_Error_KeyAlreadyExists_Title"), Messages.getFormattedString("HerokuAppInformationEnvironment_Error_KeyAlreadyExists", entry.getKey())); //$NON-NLS-1$ //$NON-NLS-2$
							return;
						}
					}

					if (doAddEnv(shell, key, value)) {
						super.okPressed();
						refreshEnvVariables();
					}
				}
				else {
					HerokuUtils
							.userError(
									shell,
									Messages.getString("HerokuAppInformationEnvironment_Error_MissingInput_Title"), Messages.getString("HerokuAppInformationEnvironment_Error_MissingInput")); //$NON-NLS-1$ //$NON-NLS-2$
					keyField.setFocus();
					if (HerokuUtils.isNotEmpty(key)) {
						valueField.setFocus();
					}
				}
			}
		};

		d.open();
	}

	/**
	 * Adds the given Map of environment vars
	 * 
	 * @param sshKey
	 */
	private boolean doAddEnv(Shell shell, final String key, final String value) {
		boolean rv = false;
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuAppInformationEnvironment_Progress_AddingEnv"), 2); //$NON-NLS-1$
					monitor.worked(1);
					try {

						ArrayList<KeyValue> envList = new ArrayList<KeyValue>();
						envList.add(new KeyValue(key, value));

						Activator.getDefault().getService().addEnvVariables(monitor, domainObject, envList);
						monitor.worked(1);
						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});

			return true;
		}
		catch (InvocationTargetException e1) {
			if ((e1.getCause() instanceof HerokuServiceException)) {
				HerokuServiceException e2 = (HerokuServiceException) e1.getCause();

				if (e2.getErrorCode() == HerokuServiceException.REQUEST_FAILED) {
					HerokuUtils
							.userError(
									shell,
									Messages.getString("HerokuAppInformationEnvironment_Error_KeyOrValueInvalid_Title"), Messages.getString("HerokuAppInformationEnvironment_Error_KeyOrValueInvalid")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else {
					Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to add new environment variable", e1); //$NON-NLS-1$
					HerokuUtils.herokuError(shell, e2);
				}
			}
			else {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to add new environment variable", e1); //$NON-NLS-1$
				HerokuUtils.internalError(shell, e1);
			}
		}
		catch (InterruptedException e1) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to add new environment variable", e1); //$NON-NLS-1$
			HerokuUtils.internalError(shell, e1);
		}

		return rv;
	}

	/**
	 * Adds the given Map of environment vars
	 * 
	 * @param sshKey
	 */
	private boolean doRemoveEnv(Shell shell, final List<KeyValue> envList) {
		boolean rv = false;
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.getString("HerokuAppInformationEnvironment_Progress_RemovingEnv_Title"), envList.size() + 1); //$NON-NLS-1$
					monitor.worked(1);
					try {
						for (KeyValue env : envList) {
							monitor.subTask(Messages.getFormattedString("HerokuAppInformationEnvironment_Progress_RemovingEnv", env.getKey())); //$NON-NLS-1$
							Activator.getDefault().getService().removeEnvVariable(monitor, domainObject, env.getKey());
							monitor.worked(1);
						}
						monitor.done();
					}
					catch (HerokuServiceException e) {
						// rethrow to outer space
						throw new InvocationTargetException(e);
					}
				}
			});

			return true;
		}
		catch (InvocationTargetException e1) {
			if ((e1.getCause() instanceof HerokuServiceException)) {
				HerokuServiceException e2 = (HerokuServiceException) e1.getCause();
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to remove environment variable(s)", e2); //$NON-NLS-1$
				HerokuUtils.herokuError(shell, e2);
			}
			else {
				Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to remove new environment variable(s)", e1); //$NON-NLS-1$
				HerokuUtils.internalError(shell, e1);
			}
		}
		catch (InterruptedException e1) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unknown error when trying to remove environment variable(s)", e1); //$NON-NLS-1$
			HerokuUtils.internalError(shell, e1);
		}

		return rv;
	}

	/**
	 * @param domainObject
	 */
	public void setDomainObject(App domainObject) {
		this.domainObject = domainObject;
		refreshEnvVariables();
	}

	private void refreshEnvVariables() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						envList = Activator.getDefault().getService().listEnvVariables(monitor, domainObject);
						HerokuUtils.runOnDisplay(true, viewer, envList, ViewerOperations.input(viewer));
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

	public void dispose() {

	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}
}