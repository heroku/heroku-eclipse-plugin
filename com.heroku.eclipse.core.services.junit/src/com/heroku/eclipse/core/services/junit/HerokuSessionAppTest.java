package com.heroku.eclipse.core.services.junit;
import java.util.List;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;


public class HerokuSessionAppTest extends HerokuSessionTest {

	private final String EXISTING_FOREIGN_APP = "asdf"; // TODO we need an appname that is guaranteed to exist
	
	private final String INVALID_APP_NAME = "asdlf_asdf";
	
	private final String VALID_APP1_NAME = "junit-test-app-1-93944";
	private final String VALID_APP2_NAME = "junit-test-app-2-93944";
	
	private void destroyAllApps(HerokuSession session) throws HerokuServiceException {
		for (App app : session.listApps()) {
			session.destroyApp(app.getName());
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		HerokuSession session = getSession();
		
		// remove all apps
		destroyAllApps(session);
		// add named app
		App newApp = new App().named(VALID_APP1_NAME);
		session.createApp(newApp);
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		HerokuSession session = getSession();
		
		// remove all apps
		destroyAllApps(session);
	}
	
	public void testGetAllApps() throws Exception {
		HerokuSession session = getSession();
		List<App> apps = session.listApps();
		assertEquals("app count", 1, apps.size());
		assertEquals("app name", VALID_APP1_NAME, apps.get(0).getName());
	}
	
	public void testGetAppAppsInvalidSession() {
		HerokuSession session = getSession();
		try {
			session.listApps();
			fail("expected invalid session error");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected invalid session", HerokuServiceException.INVALID_STATE, e.getErrorCode());
		}
	}
	
	public void testCreateApp() {
		HerokuSession session = getSession();
		try {
			session.createApp();
		}
		catch (HerokuServiceException e) {
			fail("app creation should succeed");
		}
	}
	
	public void testCreateDuplicateNamedApp() {
		HerokuSession session = getSession();
		try {
			session.createApp(new App().named(VALID_APP1_NAME));
			fail("expected duplicate name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testCreateInvalidNamedApp() {
		HerokuSession session = getSession();
		try {
			session.createApp(new App().named(INVALID_APP_NAME));
			fail("expected invalid name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testCreateExistingForeignApp() {
		HerokuSession session = getSession();
		try {
			session.createApp(new App().named(EXISTING_FOREIGN_APP));
			fail("expected app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting not allowed", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
	public void testCreateNamedApp() {
		HerokuSession session = getSession();
		try {
			session.createApp(new App().named(VALID_APP2_NAME));
		}
		catch (HerokuServiceException e) {
			fail("app creation should succeed");
		}
	}
	
	public void testDestroyInvalidApp() {
		HerokuSession session = getSession();
		try {
			session.destroyApp("not-existing-app");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT FOUND exception", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testDestroyValidApp() {
		HerokuSession session = getSession();
		try {
			session.destroyApp(VALID_APP1_NAME);
		}
		catch (HerokuServiceException e) {
			fail("expected app destruction to work " + e.getMessage());
		}
	}
	
	public void testRenameApp() {
		HerokuSession session = getSession();
		try {
			session.renameApp(VALID_APP1_NAME, VALID_APP2_NAME);
		}
		catch (HerokuServiceException e) {
			fail("expected app renaming to work " + e.getMessage());
		}
	}
	
	public void testRenameInvalidApp() {
		HerokuSession session = getSession();
		try {
			session.renameApp(VALID_APP2_NAME, VALID_APP1_NAME);
			fail("expected rename to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected not found", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testRenameAppInvalidName() {
		HerokuSession session = getSession();
		try {
			session.renameApp(VALID_APP1_NAME, INVALID_APP_NAME);
			fail("expected rename to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected not acceptable", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
}
