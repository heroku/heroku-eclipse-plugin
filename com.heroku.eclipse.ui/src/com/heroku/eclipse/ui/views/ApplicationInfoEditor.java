package com.heroku.eclipse.ui.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.part.EditorPart;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.heroku.api.App;
import com.heroku.eclipse.core.constants.HerokuEditorConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.WorkbenchOperations;
import com.heroku.eclipse.ui.utils.WorkbenchOperations.IWorkbenchPartExtension;
import com.heroku.eclipse.ui.views.dialog.ApplicationInfoPart;
import com.heroku.eclipse.ui.views.dialog.CollaboratorsPart;
import com.heroku.eclipse.ui.views.dialog.EnvironmentVariablesPart;
import com.heroku.eclipse.ui.views.dialog.ProcessListingPart;
import com.heroku.eclipse.ui.views.dialog.WebsiteOpener;

/**
 * Editor container for an entire Heroku App
 * @author tom.schindl@bestsolution.at
 */
public class ApplicationInfoEditor extends EditorPart implements WebsiteOpener, IWorkbenchPartExtension {
	/**
	 * editor's public ID
	 */
	public static final String ID = "com.heroku.eclipse.ui.appinfoeditor"; //$NON-NLS-1$
	private ApplicationInfoPart infopart;
	private CollaboratorsPart collabpart;
	private EnvironmentVariablesPart envpart;
	private ProcessListingPart processPart;

	private TabFolder folder;
	private List<ServiceRegistration<EventHandler>> handlerRegistrations;

	@Override
	public void createPartControl(Composite parent) {
		folder = new TabFolder(parent, SWT.TOP | SWT.BORDER);

		{
			TabItem item = new TabItem(folder, SWT.NONE);
			item.setData(HerokuServices.ROOT_WIDGET_ID, HerokuEditorConstants.P_INFO);
			item.setText(Messages.getString("HerokuAppManagerViewPart_AppInfo")); //$NON-NLS-1$
			infopart = new ApplicationInfoPart(this);
			item.setControl(infopart.createUI(folder));
			infopart.setDomainObject(getApp());
		}

		{
			TabItem item = new TabItem(folder, SWT.NONE);
			item.setData(HerokuServices.ROOT_WIDGET_ID, HerokuEditorConstants.P_COLLABORATION);
			item.setText(Messages.getString("HerokuAppManagerViewPart_Collaborators")); //$NON-NLS-1$
			collabpart = new CollaboratorsPart();
			item.setControl(collabpart.createUI(folder));
			collabpart.setDomainObject(getApp());
		}

		{
			TabItem item = new TabItem(folder, SWT.NONE);
			item.setData(HerokuServices.ROOT_WIDGET_ID, HerokuEditorConstants.P_ENVIRONMENT);
			item.setText(Messages.getString("HerokuAppManagerViewPart_EnvironmentVariables")); //$NON-NLS-1$
			envpart = new EnvironmentVariablesPart();
			item.setControl(envpart.createUI(folder));
			envpart.setDomainObject(getApp());
		}

		{
			TabItem item = new TabItem(folder, SWT.NONE);
			item.setData(HerokuServices.ROOT_WIDGET_ID, HerokuEditorConstants.P_PROCESSES);
			item.setText(Messages.getString("HerokuAppManagerViewPart_Processes")); //$NON-NLS-1$
			processPart = new ProcessListingPart();
			item.setControl(processPart.createUI(folder));
			processPart.setDomainObject(getApp());
		}

		folder.setSelection(0);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		subscribeToEvents();
	}

	private App getApp() {
		return ((ApplicationEditorInput) getEditorInput()).getApp();
	}

	private void updateApp(App app) {
		infopart.setDomainObject(app);
		collabpart.setDomainObject(app);
		envpart.setDomainObject(app);
		((ApplicationEditorInput) getEditorInput()).setApp(app);
	}

	@Override
	public void setPartName(String partName) {
		super.setPartName(partName);
	}

	private void subscribeToEvents() {
		EventHandler renameApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				if (getApp().getId().equals(event.getProperty(HerokuServices.KEY_APPLICATION_ID))) {
					try {
						App app = Activator.getDefault().getService()
								.getApp(new NullProgressMonitor(), (String) event.getProperty(HerokuServices.KEY_APPLICATION_NAME));
						updateApp(app);
					}
					catch (HerokuServiceException e) {
						HerokuUtils.herokuError(getShell(), e);
					}

					HerokuUtils.runOnDisplay(true, folder, getApp().getName(), WorkbenchOperations.setPartName(ApplicationInfoEditor.this));
				}
			}
		};

		EventHandler transferApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				if (getApp().getId().equals(event.getProperty(HerokuServices.KEY_APPLICATION_ID))) {
					try {
						App app = Activator.getDefault().getService().getApp(new NullProgressMonitor(), getApp().getName());
						updateApp(app);
					}
					catch (HerokuServiceException e) {
						HerokuUtils.herokuError(getShell(), e);
					}
				}
			}
		};

		EventHandler destroyedApplicationHandler = new EventHandler() {

			@Override
			public void handleEvent(Event event) {
				if (getApp().getId().equals(event.getProperty(HerokuServices.KEY_APPLICATION_ID))) {
					HerokuUtils.runOnDisplay(true, folder, ApplicationInfoEditor.this,
							WorkbenchOperations.close(getSite().getWorkbenchWindow().getActivePage()));
				}
			}
		};

		handlerRegistrations = new ArrayList<ServiceRegistration<EventHandler>>();
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(renameApplicationHandler, HerokuServices.TOPIC_APPLICATION_RENAMED));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(transferApplicationHandler, HerokuServices.TOPIC_APPLICATION_TRANSFERED));
		handlerRegistrations.add(Activator.getDefault().registerEvenHandler(destroyedApplicationHandler, HerokuServices.TOPIC_APPLICATION_DESTROYED));
	}

	public void dispose() {
		super.dispose();

		if (handlerRegistrations != null) {
			for (ServiceRegistration<EventHandler> r : handlerRegistrations) {
				r.unregister();
			}
		}
	}

	@Override
	public void setFocus() {
		if (folder.getSelectionIndex() == 0) {
			infopart.setFocus();
		}
		else if (folder.getSelectionIndex() == 1) {
			collabpart.setFocus();
		}
		else if (folder.getSelectionIndex() == 2) {
			envpart.setFocus();
		}
		else {
			folder.setFocus();
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(getApp().getName());
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

	Shell getShell() {
		return getSite().getWorkbenchWindow().getShell();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
}
