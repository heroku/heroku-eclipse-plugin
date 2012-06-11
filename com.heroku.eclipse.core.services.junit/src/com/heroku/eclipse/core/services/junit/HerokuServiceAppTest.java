package com.heroku.eclipse.core.services.junit;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;


public class HerokuServiceAppTest extends HerokuServicesTest {

	private final String EXISTING_APP = "MyHerokuApp";
	
	private final String INVALID_APP_NAME = "asdlf_asdf";
	
	private final String VALID_APP1_NAME = "junit-test-app-1-93944";
	private final String VALID_APP2_NAME = "junit-test-app-2-93944";
	
	private void destroyAllApps(HerokuServices service) throws HerokuServiceException {
		for (App app : service.listApps()) {
			service.destroyApplication(app);
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		HerokuServices service = getService();
		
		// remove all apps
		destroyAllApps(service);
		// add a simple named app
		App newApp = new App().named(VALID_APP1_NAME);
		service.getOrCreateHerokuSession().createApp(newApp);
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		HerokuServices service = getService();
		
		// remove all apps
		destroyAllApps(service);
	}
	
	public void testListApps() throws Exception {
		HerokuServices service = getService();
		
		try {
			List<App> apps = service.listApps();
			assertEquals("app count", 1, apps.size());
			assertEquals("app name", VALID_APP1_NAME, apps.get(0).getName());
		}
		catch ( HerokuServiceException e ) {
			fail("apps listing should be possible");
		}
	}
	
//	public void testGetAppAppsInvalidSession() {
//		HerokuServices service = getService();
//		try {
//			session.listApps();
//			fail("expected invalid session error");
//		}
//		catch (HerokuServiceException e) {
//			assertEquals("expected invalid session", HerokuServiceException.INVALID_SESSION, e.getErrorCode());
//		}
//	}
	
	public void testCreateDuplicateNamedApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().createApp(new App().named(VALID_APP1_NAME));
			fail("expected duplicate name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testCreateInvalidNamedApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().createApp(new App().named(INVALID_APP_NAME));
			fail("expected invalid name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testCreateExistingForeignApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().createApp(new App().named(EXISTING_APP));
			fail("expected app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting not allowed", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
	public void testCreateNamedApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().createApp(new App().named(VALID_APP2_NAME));
		}
		catch (HerokuServiceException e) {
			fail("app creation should succeed");
		}
	}
	
	public void testDestroyInvalidApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().destroyApp("not-existing-app");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT FOUND exception", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testDestroyValidApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().destroyApp(VALID_APP1_NAME);
		}
		catch (HerokuServiceException e) {
			fail("expected app destruction to work " + e.getMessage());
		}
	}
	
	public void testRenameApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().renameApp(VALID_APP1_NAME, VALID_APP2_NAME);
		}
		catch (HerokuServiceException e) {
			fail("expected app renaming to work " + e.getMessage());
		}
	}
	
	public void testRenameInvalidApp() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().renameApp(VALID_APP2_NAME, VALID_APP1_NAME);
			fail("expected rename to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected not found", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testRenameAppInvalidName() {
		HerokuServices service = getService();
		try {
			service.getOrCreateHerokuSession().renameApp(VALID_APP1_NAME, INVALID_APP_NAME);
			fail("expected rename to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected not acceptable", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
}
