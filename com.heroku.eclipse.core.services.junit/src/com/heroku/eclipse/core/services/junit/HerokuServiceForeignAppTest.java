package com.heroku.eclipse.core.services.junit;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.junit.common.Credentials;
import com.heroku.eclipse.core.services.junit.common.HerokuTestConstants;


public class HerokuServiceForeignAppTest extends HerokuServicesTest {
	private void destroyAllOwnApps(IProgressMonitor pm, HerokuServices service) throws HerokuServiceException {
		if ( service.isReady(pm) ) {
			for (App app : service.listApps(pm)) {
				if ( service.isOwnApp(pm, app)) {
					service.destroyApplication(pm, app);
				}
			}
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		IProgressMonitor pm = getProgressMonitor();
		HerokuServices service = getService();
		service.setAPIKey(pm, Credentials.VALID_JUNIT_APIKEY1);
		service.setSSHKey(pm, Credentials.VALID_PUBLIC_SSH_KEY1);
		
		// remove all apps
		destroyAllOwnApps(pm, service);
		
		// add a simple named app
		App newApp = new App().named(HerokuTestConstants.VALID_APP1_NAME);
		service.getOrCreateHerokuSession(pm).createApp(newApp);
		
		service.addCollaborator(pm, newApp, Credentials.VALID_JUNIT_USER2);
		service.setAPIKey(pm, Credentials.VALID_JUNIT_APIKEY2);
		service.setSSHKey(pm, Credentials.VALID_PUBLIC_SSH_KEY2);
	}
	
	protected App getValidForeignDummyApp() throws HerokuServiceException {
		return getService().getApp(new NullProgressMonitor(), HerokuTestConstants.VALID_APP1_NAME);
	}
	
	@Override
	protected void tearDown() throws Exception {
		IProgressMonitor pm = getProgressMonitor();
		HerokuServices service = getService();
		
		// remove all apps of second user
		destroyAllOwnApps(pm, service);

		// remove all apps of first user
		service.setAPIKey(pm, Credentials.VALID_JUNIT_APIKEY1);
		service.setSSHKey(pm, Credentials.VALID_PUBLIC_SSH_KEY1);
		destroyAllOwnApps(pm, service);

		service.setSSHKey(pm, null);
		service.setAPIKey(pm, null);
	}
	
	public void testListForeignApps() throws Exception {
		HerokuServices service = getService();
		
		try {
			List<App> apps = service.listApps(getProgressMonitor());
			assertEquals("app count", 1, apps.size());
			assertEquals("expecting foreign app not to be owned by current user", false, service.isOwnApp(getProgressMonitor(), apps.get(0)));
			assertEquals("app name", HerokuTestConstants.VALID_APP1_NAME, apps.get(0).getName());
		}
		catch ( HerokuServiceException e ) {
			e.printStackTrace();
			fail("apps listing should be possible "+e.getMessage());
		}
	}
	
	public void testDestroyForeignApp() {
		HerokuServices service = getService();
		try {
			service.destroyApplication(getProgressMonitor(), getValidForeignDummyApp());
			fail("expected foreign app destruction to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
	public void testRenameForeignApp() {
		HerokuServices service = getService();
		try {
			service.renameApp(getProgressMonitor(), getValidForeignDummyApp(), HerokuTestConstants.VALID_APP2_NAME);
			fail("expected foreign app renaming to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
//	public void testScaleForeignApp() {
//	}

}
