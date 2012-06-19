package com.heroku.eclipse.core.services.junit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Ignore;
import org.junit.Test;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;


public class HerokuServiceForeignAppTest extends HerokuServicesTest {
	
	private final String EXISTING_APP = "myherokuapp";
	
	private final String INVALID_APP_NAME = "asdlf_asdf";
	
	private final String VALID_APP1_NAME = "junit-test-app-1-93944";
	private final String VALID_APP2_NAME = "junit-test-app-2-93944";
	
	private final String NON_EXISTING_APP_NAME = "junit-test-app-3-93944";
	
	private void destroyAllOwnApps(HerokuServices service) throws HerokuServiceException {
		if ( service.isReady() ) {
			for (App app : service.listApps()) {
				if ( service.isOwnApp(app)) {
					service.destroyApplication(app);
				}
			}
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		HerokuServices service = getService();
		service.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
		service.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		
		// remove all apps
		destroyAllOwnApps(service);
		
		// add a simple named app
		App newApp = new App().named(VALID_APP1_NAME);
		service.getOrCreateHerokuSession().createApp(newApp);
		
		service.addCollaborator(newApp, Credentials.VALID_JUNIT_USER2);
		service.setAPIKey(Credentials.VALID_JUNIT_APIKEY2);
		service.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY2);
	}
	
	protected App getValidForeignDummyApp() throws HerokuServiceException {
		return getService().getApp(VALID_APP1_NAME);
	}
	
	@Override
	protected void tearDown() throws Exception {
		HerokuServices service = getService();
		
		// remove all apps of second user
		destroyAllOwnApps(service);

		// remove all apps of first user
		service.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
		service.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		destroyAllOwnApps(service);
	}
	
	public void testListForeignApps() throws Exception {
		HerokuServices service = getService();
		
		try {
			List<App> apps = service.listApps();
			assertEquals("app count", 1, apps.size());
			assertEquals("expecting foreign app not to be owned by current user", false, service.isOwnApp(apps.get(0)));
			assertEquals("app name", VALID_APP1_NAME, apps.get(0).getName());
		}
		catch ( HerokuServiceException e ) {
			fail("apps listing should be possible");
		}
	}
	
	public void testDestroyForeignApp() {
		HerokuServices service = getService();
		try {
			service.destroyApplication(getValidForeignDummyApp());
			fail("expected foreign app destruction to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
	public void testRenameForeignApp() {
		HerokuServices service = getService();
		try {
			service.renameApp(getValidForeignDummyApp(), VALID_APP2_NAME);
			fail("expected foreign app renaming to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
//	public void testScaleForeignApp() {
//	}

}
