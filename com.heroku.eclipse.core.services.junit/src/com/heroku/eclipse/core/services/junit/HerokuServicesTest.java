package com.heroku.eclipse.core.services.junit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

import junit.framework.TestCase;

public class HerokuServicesTest extends TestCase {
	
	private static final String VALID_JUNIT_USER = System.getProperty("heroku.junit.user");
	private static final String VALID_JUNIT_PWD = System.getProperty("heroku.junit.pwd");
	private static final String VALID_JUNIT_APIKEY = System.getProperty("heroku.junit.apikey");
	
	private HerokuServices getService() {
		Bundle b = FrameworkUtil.getBundle(HerokuServicesTest.class);
		BundleContext btx = b.getBundleContext();
		
		ServiceReference<HerokuServices> ref = null;
		ref = btx.getServiceReference(HerokuServices.class);
		return btx.getService(ref);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// Ensure we have clean preferences
		HerokuServices s = getService();
		s.setAPIKey(null);
		s.setSSHKey(null);
	}
	
	public void testObtainAPIKey() {
		HerokuServices h = getService();
		
		try {
			h.obtainAPIKey("nouser@noaddres.com", "nopassword"); //$NON-NLS-1$ //$NON-NLS-2$
			fail("The login with nouser@noaddres.com/nopassword has to fail"); //$NON-NLS-1$
		} catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e.getErrorCode());
			assertNotNull(e.getCause());
		}
		
		try {
			h.obtainAPIKey(VALID_JUNIT_USER, "nopassword"); //$NON-NLS-1$ //$NON-NLS-2$
			fail("The login has to fail because the password for eclipse-junit@bestsolution.at is different to 'nopassword'"); //$NON-NLS-1$
		} catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e.getErrorCode());
			assertNotNull(e.getCause());
		}
		
		try {
			String apiKey = h.obtainAPIKey(VALID_JUNIT_USER, VALID_JUNIT_PWD); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull(apiKey);
			assertEquals(VALID_JUNIT_APIKEY, apiKey);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testGetAPIKey() {
		HerokuServices h = getService();
		assertNull(h.getAPIKey());
		try {
			h.setAPIKey(VALID_JUNIT_APIKEY);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertNotNull(h.getAPIKey());
	}
	
	public void testValidateAPIKey() {
		HerokuServices h = getService();
		try {
			h.validateAPIKey(VALID_JUNIT_APIKEY);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		try {
			h.validateAPIKey("noapikeyatall");
			fail("The key 'noapikeyatall' is a valid API key");
		} catch (HerokuServiceException e) {
			assertEquals("An invalid key error should be thrown", HerokuServiceException.INVALID_API_KEY, e.getErrorCode());
		}
	}
	
	public void testSetAPIKey() {
		HerokuServices h = getService();
		try {
			h.setAPIKey(VALID_JUNIT_APIKEY);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		try {
			h.setAPIKey("noapikeyatall");
			fail("The key 'noapikeyatall' is a valid API key");
		} catch (HerokuServiceException e) {
			assertEquals("Setting should fail with an invalid key command", HerokuServiceException.INVALID_API_KEY, e.getErrorCode());
		}
		
		try {
			h.setAPIKey("");
			h.setAPIKey(null);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testGetSSHKey() {
		HerokuServices h = getService();
		assertNull(h.getSSHKey());
		try {
			h.setSSHKey("TBD");
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertNotNull(h.getSSHKey());
		
		try {
			h.setSSHKey("");
			h.setSSHKey(null);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testGetOrCreateHerokuSession() {
		HerokuServices h = getService();
		
		try {
			h.getOrCreateHerokuSession();
			fail("There's no API key configured so the tests should fail");
		} catch (HerokuServiceException e) {
			assertEquals("Exception should say there's no API key", HerokuServiceException.NO_API_KEY, e.getErrorCode());
		}
		
		try {
			h.setAPIKey(VALID_JUNIT_APIKEY);
			HerokuSession session = h.getOrCreateHerokuSession();
			assertNotNull(session);
			assertSame(session, h.getOrCreateHerokuSession());
			assertTrue("The session should be valid", session.isValid());
			h.setAPIKey(VALID_JUNIT_APIKEY);
			assertTrue("The session should be still valid because the key hasn't changed", session.isValid());
//TODO Need a second test account			
//			h.setAPIKey("NEED A 2nd API Key");
//			assertFalse("The session should be invalidated because the API-key changed", session.isValid());
//			assertNotSame(session, h.getOrCreateHerokuSession());
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}