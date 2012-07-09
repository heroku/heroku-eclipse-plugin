package com.heroku.eclipse.ui.views.dialog;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.messages.Messages;
import com.heroku.eclipse.ui.utils.HerokuUtils;
import com.heroku.eclipse.ui.utils.RunnableWithParameter;

/**
 * @author tom.schindl@bestsolution.at
 */
public class ApplicationInfoPart {
	private App domainObject;
	private Text appName;
	private Link appUrl;
	private Label appGitUrl;
	private Label appDomainName;
	private Button renameApp;

	private WebsiteOpener websiteOpener;

	public ApplicationInfoPart(WebsiteOpener websiteOpener) {
		this.websiteOpener = websiteOpener;
	}

	/**
	 * @param parent
	 * @return
	 */
	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		{
			Label l = new Label(container, SWT.NONE);
			l.setText(Messages.getString("HerokuAppInformationPart_Name")); //$NON-NLS-1$
			appName = new Text(container, SWT.BORDER);
			appName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			renameApp = new Button(container, SWT.PUSH);
			renameApp.setText(Messages.getString("HerokuAppInformationPart_Rename")); //$NON-NLS-1$
			renameApp.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						Activator.getDefault().getLogger()
								.log(LogService.LOG_INFO, "about to rename app from '" + domainObject.getName() + "' to '" + appName.getText() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						Activator.getDefault().getService().renameApp(new NullProgressMonitor(), domainObject, appName.getText());
						Activator.getDefault().getLogger().log(LogService.LOG_INFO, "app rename complete"); //$NON-NLS-1$
					}
					catch (HerokuServiceException e1) {
						if (e1.getErrorCode() == HerokuServiceException.NOT_ACCEPTABLE) {
							Activator
									.getDefault()
									.getLogger()
									.log(LogService.LOG_WARNING,
											"new app name '" + appName.getText() + "' either already exists or is invalid, rejecting rename!"); //$NON-NLS-1$ //$NON-NLS-2$
							HerokuUtils.userError(
									renameApp.getShell(),
									Messages.getString("HerokuAppInformationPart_Error_NameAlreadyExists_Title"), Messages.getString("HerokuAppInformationPart_Error_NameAlreadyExists")); //$NON-NLS-1$ //$NON-NLS-2$
						}
						else {
							HerokuUtils.herokuError(renameApp.getShell(), e1);
						}
					}
				}
			});
		}

		{
			Label l = new Label(container, SWT.NONE);
			l.setText(Messages.getString("HerokuAppInformationPart_URL")); //$NON-NLS-1$
			appUrl = new Link(container, SWT.NONE);
			appUrl.setLayoutData(new GridData(GridData.FILL, SWT.CENTER, true, false, 2, 1));
			appUrl.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					websiteOpener.openInternal(domainObject);
				}
			});
		}

		{
			Label l = new Label(container, SWT.NONE);
			l.setText(Messages.getString("HerokuAppInformationPart_GitRepositoryURL")); //$NON-NLS-1$
			appGitUrl = new Label(container, SWT.NONE);
			appGitUrl.setLayoutData(new GridData(GridData.FILL, SWT.CENTER, true, false, 2, 1));
		}

		{
			Label l = new Label(container, SWT.NONE);
			l.setText(Messages.getString("HerokuAppInformationPart_DomainName")); //$NON-NLS-1$
			appDomainName = new Label(container, SWT.NONE);
			appDomainName.setLayoutData(new GridData(GridData.FILL, SWT.CENTER, true, false, 2, 1));
		}

		return container;
	}

	public void setDomainObject(App domainObject) {
		this.domainObject = domainObject;
		HerokuUtils.runOnDisplay(true, appUrl, domainObject, new RunnableWithParameter<App>() {

			@Override
			public void run(App argument) {
				appName.setText(HerokuUtils.ensureNotNull(argument.getName()));
				appUrl.setText("<a>" + HerokuUtils.ensureNotNull(argument.getWebUrl()) + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
				appGitUrl.setText(HerokuUtils.ensureNotNull(argument.getGitUrl()));
				appDomainName.setText(argument.getDomain() == null ? "" : HerokuUtils.ensureNotNull(argument.getDomain().getDomain())); //$NON-NLS-1$
			}
		});

	}

	public void dispose() {

	}

	public void setFocus() {
		// TODO Auto-generated method stub

	}
}
