package com.heroku.eclipse.core.services.junit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.User;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.junit.common.Credentials;
import com.heroku.eclipse.core.services.junit.common.HerokuTestConstants;
import com.heroku.eclipse.core.services.model.AppTemplate;


public class HerokuServiceOwnAppTest extends HerokuServicesTest {
	
	private List<AppTemplate> templatesList = null;
	
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
	}
	
	protected AppTemplate getTestTemplate() throws HerokuServiceException {
		if ( templatesList == null ) {
			templatesList = getService().listTemplates(getProgressMonitor());
			if ( templatesList.size() <= 0 ) {
				throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "unable to list templates");
			}
		}
		
		return templatesList.get(0);
	}
	
	protected App getValidDummyApp() throws HerokuServiceException {
		return getService().getApp(getProgressMonitor(), HerokuTestConstants.VALID_APP1_NAME);
	}
	
	@Override
	protected void tearDown() throws Exception {
		IProgressMonitor pm = getProgressMonitor();
		HerokuServices service = getService();
		
		// remove all apps
		destroyAllOwnApps(pm, service);

		if ( templatesList != null ) {
			templatesList.clear();
		}
		service.setAPIKey(pm, null);
		service.setSSHKey(pm, null);
	}
	
	public void testGetUserInfo() {
		HerokuServices service = getService();
		try {
			User user = service.getUserInfo(getProgressMonitor());
			assertEquals("fetched user is not identical to the currenctly logged in user", Credentials.VALID_JUNIT_USER1, user.getEmail());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expected fetching user info to succeed: "+e.getMessage());
		}
	}
	
	public void testGetApp() {
		HerokuServices service = getService();
		try {
			App app = service.getApp(getProgressMonitor(), HerokuTestConstants.VALID_APP1_NAME);
			assertEquals("fetched app is not expected, original app", HerokuTestConstants.VALID_APP1_NAME, app.getName());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expected fetching app info to succeed: "+e.getMessage());
		}
	}
	
	public void testListOwnApps() throws Exception {
		HerokuServices service = getService();
		
		try {
			List<App> ownApps = new ArrayList<App>();
			for (App app : service.listApps(getProgressMonitor())) {
				if ( service.isOwnApp(getProgressMonitor(), app)) {
					ownApps.add(app);
				}
			}
			assertEquals("app count", 1, ownApps.size());
			assertEquals("app name", HerokuTestConstants.VALID_APP1_NAME, ownApps.get(0).getName());
		}
		catch ( HerokuServiceException e ) {
			e.printStackTrace();
			fail("apps listing should be possible: "+e.getMessage());
		}
	}
	
	public void testListTemplates() {
		HerokuServices service = getService();
		try {
			List<AppTemplate> templates = service.listTemplates(getProgressMonitor());
			assertTrue("expecting templates list to contain at least one template", templates.size()>0);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("templates listing must succeed "+e.getMessage());
		}
	}
	
	public void testCreateNamedAppFromTemplate() {
		HerokuServices service = getService();
		try {
			App newApp = service.createAppFromTemplate(getProgressMonitor(), HerokuTestConstants.VALID_APP2_NAME, getTestTemplate().getTemplateName());
			assertNotNull(newApp);
			App testApp = service.getApp(getProgressMonitor(), HerokuTestConstants.VALID_APP2_NAME);
			assertEquals("new materialized app is not the same as the remote one", newApp.getId(), testApp.getId());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("app creation from template should succeed "+e.getMessage());
		}
	}
	
	public void testCreateDuplicateNamedAppFromTemplate() {
		HerokuServices service = getService();
		try {
			service.createAppFromTemplate(getProgressMonitor(), HerokuTestConstants.VALID_APP1_NAME, getTestTemplate().getTemplateName());
			fail("expected duplicate name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testCreateInvalidNamedApp() {
		HerokuServices service = getService();
		try {
			service.createAppFromTemplate(getProgressMonitor(), HerokuTestConstants.INVALID_APP_NAME, getTestTemplate().getTemplateName());
			fail("expected invalid name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testCreateExistingForeignApp() {
		HerokuServices service = getService();
		try {
			service.createAppFromTemplate(getProgressMonitor(), HerokuTestConstants.EXISTING_FOREIGN_APP, getTestTemplate().getTemplateName());
			fail("expected app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("creation of already existing app must fail", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testDestroyInvalidApp() {
		HerokuServices service = getService();
		try {
			service.destroyApplication(getProgressMonitor(), new App().named(HerokuTestConstants.NON_EXISTING_APP_NAME));
			fail("expected non existing app destruction to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT FOUND exception", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testDestroyValidApp() {
		HerokuServices service = getService();
		try {
			service.destroyApplication(getProgressMonitor(), getValidDummyApp());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expected app destruction to work " + e.getMessage());
		}
	}
	
	public void testRenameApp() {
		HerokuServices service = getService();
		try {
			service.renameApp(getProgressMonitor(), getValidDummyApp(), HerokuTestConstants.VALID_APP2_NAME);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expected app renaming to work " + e.getMessage());
		}
	}
	
	public void testRenameAppInvalidName() {
		HerokuServices service = getService();
		try {
			service.renameApp(getProgressMonitor(), getValidDummyApp(), HerokuTestConstants.INVALID_APP_NAME);
			fail("expected rename to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected not acceptable", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testAddAndListCollaborators() {
		HerokuServices service = getService();
		try {
			service.addCollaborator(getProgressMonitor(), getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
			List<Collaborator> collaborators = service.getCollaborators(getProgressMonitor(), getValidDummyApp());
			// 2 collaborators because the owner is listed as a collaborator as well
			assertEquals("expecting to have exactly 2 collaborators", 2, collaborators.size());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expecting adding collaborator to succeed: "+e.getMessage());
		}
		
	}
	
	public void testAddInvalidCollaborator() {
		HerokuServices service = getService();
		try {
			service.addCollaborator(getProgressMonitor(), getValidDummyApp(), "this is no email address");
			fail("expected adding invalid collaborator to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("adding invalid collaborator must fail", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testRemoveCollaborator() {
		HerokuServices service = getService();
		try {
			service.addCollaborator(getProgressMonitor(), getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
			service.removeCollaborators(getProgressMonitor(), getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("adding and removing valid collaborator must succeed: "+e.getMessage());
		}
	}
	
	public void testRemoveUnknownCollaborator() {
		HerokuServices service = getService();
		try {
			service.removeCollaborators(getProgressMonitor(), getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
			fail("removal of unknown collaborator must fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting removal of unknown collaborator to die with REQUEST FAILED", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testRestartApplication() {
		HerokuServices service = getService();
		try {
			service.restartApplication(getProgressMonitor(), getValidDummyApp());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expecting app restart to succeed: "+e.getMessage());
		}
	}

//	public void testMaterializeGitMavenApp() {
//
//	}
//
//	public void testMaterializeGitGeneralApp() {
//
//	}
//
//	public void testCreateProject() {
//
//	}
//
//	public void testViewLogs() {
//		
//	}
//
//	public void testScaleApp() {
//
//	}
//
//	public void testTransferApplication() {
//		
//	}
}
