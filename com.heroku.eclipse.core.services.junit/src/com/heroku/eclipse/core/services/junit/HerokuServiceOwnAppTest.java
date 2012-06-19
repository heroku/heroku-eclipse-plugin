package com.heroku.eclipse.core.services.junit;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.User;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;


public class HerokuServiceOwnAppTest extends HerokuServicesTest {
	
	private List<AppTemplate> templatesList = null;
	
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
		
		// remove all apps
		destroyAllOwnApps(service);
		
		// add a simple named app
		App newApp = new App().named(VALID_APP1_NAME);
		service.getOrCreateHerokuSession().createApp(newApp);
	}
	
	protected AppTemplate getTestTemplate() throws HerokuServiceException {
		if ( templatesList == null ) {
			templatesList = getService().listTemplates();
			if ( templatesList.size() <= 0 ) {
				throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "unable to list templates");
			}
		}
		
		return templatesList.get(0);
	}
	
	protected App getValidDummyApp() throws HerokuServiceException {
		return getService().getApp(VALID_APP1_NAME);
	}
	
	@Override
	protected void tearDown() throws Exception {
		HerokuServices service = getService();
		
		// remove all apps
		destroyAllOwnApps(service);

		if ( templatesList != null ) {
			templatesList.clear();
		}
	}
	
	public void testGetUserInfo() {
		HerokuServices service = getService();
		try {
			User user = service.getUserInfo();
			assertEquals("fetched user is not identical to the currenctly logged in user", Credentials.VALID_JUNIT_USER1, user.getEmail());
		}
		catch (HerokuServiceException e) {
			fail("expected fetching user info to succeed: "+e.getMessage());
		}
	}
	
	public void testGetApp() {
		HerokuServices service = getService();
		try {
			App app = service.getApp(VALID_APP1_NAME);
			assertEquals("fetched app is not expected, original app", VALID_APP1_NAME, app.getName());
		}
		catch (HerokuServiceException e) {
			fail("expected fetching app info to succeed: "+e.getMessage());
		}
	}
	
	public void testListOwnApps() throws Exception {
		HerokuServices service = getService();
		
		try {
			List<App> ownApps = new ArrayList<App>();
			for (App app : service.listApps()) {
				if ( service.isOwnApp(app)) {
					ownApps.add(app);
				}
			}
			assertEquals("app count", 1, ownApps.size());
			assertEquals("app name", VALID_APP1_NAME, ownApps.get(0).getName());
		}
		catch ( HerokuServiceException e ) {
			fail("apps listing should be possible");
		}
	}
	
	public void testListTemplates() {
		HerokuServices service = getService();
		try {
			List<AppTemplate> templates = service.listTemplates();
			assertTrue("expecting templates list to contain at least one template", templates.size()>0);
		}
		catch (HerokuServiceException e) {
			fail("templates listing must succeed");
		}
	}
	
	public void testCreateNamedAppFromTemplate() {
		HerokuServices service = getService();
		try {
			App newApp = service.createAppFromTemplate(VALID_APP2_NAME, getTestTemplate().getTemplateName(), new NullProgressMonitor());
			assertNotNull(newApp);
			App testApp = service.getApp(VALID_APP2_NAME);
			assertEquals("new materialized app is not the same as the remote one", newApp.getId(), testApp.getId());
		}
		catch (HerokuServiceException e) {
			fail("app creation from template should succeed");
		}
	}
	
	public void testCreateDuplicateNamedAppFromTemplate() {
		HerokuServices service = getService();
		try {
			service.createAppFromTemplate(VALID_APP1_NAME, getTestTemplate().getTemplateName(), new NullProgressMonitor());
			fail("expected duplicate name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testCreateInvalidNamedApp() {
		HerokuServices service = getService();
		try {
			service.createAppFromTemplate(INVALID_APP_NAME, getTestTemplate().getTemplateName(), new NullProgressMonitor());
			fail("expected invalid name app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting request failed", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testCreateExistingForeignApp() {
		HerokuServices service = getService();
		try {
			service.createAppFromTemplate(EXISTING_APP, getTestTemplate().getTemplateName(), new NullProgressMonitor());
			fail("expected app creation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("creation of already existing app must fail", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testDestroyInvalidApp() {
		HerokuServices service = getService();
		try {
			service.destroyApplication(new App().named(NON_EXISTING_APP_NAME));
			fail("expected non existing app destruction to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected NOT FOUND exception", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testDestroyValidApp() {
		HerokuServices service = getService();
		try {
			service.destroyApplication(getValidDummyApp());
		}
		catch (HerokuServiceException e) {
			fail("expected app destruction to work " + e.getMessage());
		}
	}
	
	public void testRenameApp() {
		HerokuServices service = getService();
		try {
			service.renameApp(getValidDummyApp(), VALID_APP2_NAME);
		}
		catch (HerokuServiceException e) {
			fail("expected app renaming to work " + e.getMessage());
		}
	}
	
	public void testRenameAppInvalidName() {
		HerokuServices service = getService();
		try {
			service.renameApp(getValidDummyApp(), INVALID_APP_NAME);
			fail("expected rename to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expected not acceptable", HerokuServiceException.NOT_ACCEPTABLE, e.getErrorCode());
		}
	}
	
	public void testAddAndListCollaborators() {
		HerokuServices service = getService();
		try {
			service.addCollaborator(getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
			List<Collaborator> collaborators = service.getCollaborators(getValidDummyApp());
			assertEquals("expecting to have exactly 1 collaborator", 1, collaborators.size());
		}
		catch (HerokuServiceException e) {
			fail("expecting adding collaborator to succeed: "+e.getMessage());
		}
		
	}
	
	public void testAddInvalidCollaborator() {
		HerokuServices service = getService();
		try {
			service.addCollaborator(getValidDummyApp(), "this is no email address");
			fail("expected adding invalid collaborator to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("adding invalid collaborator must fail", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
		}
	}
	
	public void testRemoveCollaborator() {
		HerokuServices service = getService();
		try {
			service.addCollaborator(getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
			service.removeCollaborators(getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("adding and removing valid collaborator must succeed: "+e.getMessage());
		}
	}
	
	public void testRemoveUnknownCollaborator() {
		HerokuServices service = getService();
		try {
			service.removeCollaborators(getValidDummyApp(), Credentials.VALID_JUNIT_USER2);
			fail("removal of unknown collaborator must fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting removal of unknown collaborator to die with NOT FOUND", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
	}
	
	public void testRestartApplication() {
		HerokuServices service = getService();
		try {
			service.restartApplication(getValidDummyApp());
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
