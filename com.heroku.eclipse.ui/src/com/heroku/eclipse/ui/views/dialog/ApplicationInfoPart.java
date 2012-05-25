package com.heroku.eclipse.ui.views.dialog;


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

import com.heroku.api.App;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.ui.Activator;
import com.heroku.eclipse.ui.utils.HerokuUtils;

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
	
	public Composite createUI(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3,false));
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("Name");
			appName = new Text(container, SWT.BORDER);
			appName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			renameApp = new Button(container, SWT.PUSH);
			renameApp.setText("Rename");
			renameApp.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						Activator.getDefault().getService().renameApp(domainObject, appName.getText());
					} catch (HerokuServiceException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
		}
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("URL");
			appUrl = new Link(container, SWT.NONE);
			appUrl.setLayoutData(new GridData(GridData.FILL,SWT.CENTER,true,false,2,1));
			appUrl.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					websiteOpener.openInternal(domainObject);
				}
			});
		}
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("Git Repository URL");
			appGitUrl = new Label(container, SWT.NONE);
			appGitUrl.setLayoutData(new GridData(GridData.FILL,SWT.CENTER,true,false,2,1));
		}
		
		{
			Label l = new Label(container, SWT.NONE);
			l.setText("Domain Name");
			appDomainName = new Label(container, SWT.NONE);
			appDomainName.setLayoutData(new GridData(GridData.FILL,SWT.CENTER,true,false,2,1));
		}
		
		return container;
	}
	
	public void setDomainObject(App domainObject) {
		this.domainObject = domainObject;
		appName.setText(HerokuUtils.notNull(domainObject.getName()));
		appUrl.setText("<a>" + HerokuUtils.notNull(domainObject.getWebUrl())+"</a>");
		appGitUrl.setText(HerokuUtils.notNull(domainObject.getGitUrl()));
		appDomainName.setText(domainObject.getDomain() == null ? "" : HerokuUtils.notNull(domainObject.getDomain().getDomain()));
	}
}
