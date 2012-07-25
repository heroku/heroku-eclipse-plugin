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
 * Displays basic information about an Heroku App
 * 
 * @author tom.schindl@bestsolution.at
 */
public class ApplicationInfoPart {
	private App domainObject;
	private Text appName;
	private Text appUrl;
	private Text appGitUrl;
	private Text appDomainName;
	private Button renameApp;
	private Composite parent;

	private WebsiteOpener websiteOpener;

	/**
	 * @param websiteOpener
	 */
	public ApplicationInfoPart(WebsiteOpener websiteOpener) {
		this.websiteOpener = websiteOpener;
	}

	/**
	 * @param parent
	 * @return the created UI composite
	 */
	public Composite createUI(Composite parent) {
		this.parent = parent;
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
			Link l = new Link(container, SWT.NONE);
			l.setText("<a>" + Messages.getString("HerokuAppInformationPart_URL") + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			l.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					websiteOpener.openInternal(domainObject);
				}
			});

			// Composite layoutComp = new Composite(container, SWT.NONE);
			// layoutComp.setLayoutData(new GridData(GridData.FILL, SWT.CENTER,
			// true, false, 2, 1));
			// layoutComp.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

			appUrl = new Text(container, SWT.READ_ONLY);
			appUrl.setLayoutData(new GridData(GridData.BEGINNING, SWT.CENTER, true, false, 2, 1));
			appUrl.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					websiteOpener.openInternal(domainObject);
				}
			});
			appUrl.setBackground(parent.getBackground());

			// Button b = new Button(layoutComp, SWT.PUSH);
			// b.setText("Hello World");
		}

		{
			Label l = new Label(container, SWT.NONE);
			l.setText(Messages.getString("HerokuAppInformationPart_GitRepositoryURL")); //$NON-NLS-1$
			appGitUrl = new Text(container, SWT.READ_ONLY);
			appGitUrl.setLayoutData(new GridData(GridData.BEGINNING, SWT.CENTER, true, false, 2, 1));
			appGitUrl.setEditable(false);
			appGitUrl.setBackground(parent.getBackground());
		}

		{
			Label l = new Label(container, SWT.READ_ONLY);
			l.setText(Messages.getString("HerokuAppInformationPart_DomainName")); //$NON-NLS-1$
			appDomainName = new Text(container, SWT.NONE);
			appDomainName.setLayoutData(new GridData(GridData.BEGINNING, SWT.CENTER, true, false, 2, 1));
			appDomainName.setBackground(parent.getBackground());
		}

		return container;
	}

	/**
	 * Sets this view part's domain object
	 * 
	 * @param domainObject
	 */
	public void setDomainObject(App domainObject) {
		this.domainObject = domainObject;
		HerokuUtils.runOnDisplay(true, appUrl, domainObject, new RunnableWithParameter<App>() {

			@Override
			public void run(App argument) {
				appName.setText(HerokuUtils.ensureNotNull(argument.getName()));
				appUrl.setText(HerokuUtils.ensureNotNull(argument.getWebUrl()));
				appGitUrl.setText(HerokuUtils.ensureNotNull(argument.getGitUrl()));
				appDomainName.setText(argument.getDomain() == null ? "" : HerokuUtils.ensureNotNull(argument.getDomain().getDomain())); //$NON-NLS-1$
				parent.layout(true, true);
			}
		});

	}
}
