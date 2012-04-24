package com.heroku.eclipse.core.services.junit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

import junit.framework.TestCase;

public class HerokuServicesTest extends TestCase {
	
	private HerokuServices getService() {
		Bundle b = FrameworkUtil.getBundle(HerokuServicesTest.class);
		BundleContext btx = b.getBundleContext();
		ServiceReference<HerokuServices> ref = btx.getServiceReference(HerokuServices.class);
		return btx.getService(ref);
	}
	
	public void testGetAPIKey() {
		HerokuServices h = getService();
		
		try {
			h.getAPIKey("nouser@noaddres.com", "nopassword");
			fail("The login with nouser@noaddres.com/nopassword has to fail");
		} catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e.getErrorCode());
			assertNotNull(e.getCause());
		}
		
		// TODO Need a dummy account from heroku
		try {
			h.getAPIKey("heroku.junit@bestsolution.at", "nopassword");
			fail("The login has to fail because the password for eclipse-junit@bestsolution.at is different to 'nopassword'");
		} catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e.getErrorCode());
			assertNotNull(e.getCause());
		}
		
		// TODO Need a dummy account from heroku
//		try {
//			String apiKey = h.getAPIKey("heroku.junit@bestsolution.at", "junit-pwd");
//			assertNotNull(apiKey);
//		} catch (HerokuServiceException e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
	}
}