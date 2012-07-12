package com.heroku.eclipse.core.services.junit;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.junit.common.Credentials;
import com.heroku.eclipse.core.services.junit.common.HerokuTestConstants;

public class HerokuServiceForeignAppTest extends HerokuServicesTest {
	private App foreignApp;

	private void destroyAllOwnApps(IProgressMonitor pm, HerokuServices service) throws HerokuServiceException {
		if (service.isReady(pm)) {
			for (App app : service.listApps(pm)) {
				if (service.isOwnApp(pm, app)) {
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

		foreignApp = service.getOrCreateHerokuSession(pm).createApp(new App().named(HerokuTestConstants.VALID_APP1_NAME));
		service.addCollaborator(pm, foreignApp, Credentials.VALID_JUNIT_USER2);

		service.setAPIKey(pm, Credentials.VALID_JUNIT_APIKEY2);
		service.setSSHKey(pm, Credentials.VALID_PUBLIC_SSH_KEY2);
	}

	protected App getValidForeignDummyApp() {
		return foreignApp;
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
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("apps listing should be possible " + e.getMessage());
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
			fail("expectin foreign app renaming to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expectin NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}

	public void testScaleForeignApp() {
		HerokuServices service = getService();
		try {
			service.scaleProcess(getProgressMonitor(), getValidForeignDummyApp().getName(), HerokuTestConstants.DEFAULT_DYNO_NAME, 10);
			fail("expecting foreign app scaling to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}

	public void testHighjackForeignApplication() {
		HerokuServices service = getService();
		try {
			service.transferApplication(getProgressMonitor(),getValidForeignDummyApp(), Credentials.VALID_JUNIT_USER2);
			fail("expecting foreign app transfer to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting NOT ALLOWED exception", HerokuServiceException.NOT_ALLOWED, e.getErrorCode());
		}
	}
	
	public void testTransferApplication() {
		HerokuServices service = getService();
		try {
			
			App ownApp = service.getOrCreateHerokuSession(pm).createApp(new App().named(HerokuTestConstants.VALID_APP3_NAME));

			service.addCollaborator(getProgressMonitor(), ownApp, Credentials.VALID_JUNIT_USER1);
			service.transferApplication(getProgressMonitor(),ownApp, Credentials.VALID_JUNIT_USER1);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expecting own app transfer to succeed");
		}
	}
}
