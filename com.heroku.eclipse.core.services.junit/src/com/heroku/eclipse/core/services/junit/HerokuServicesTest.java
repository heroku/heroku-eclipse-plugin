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
		
		ServiceReference<HerokuServices> ref = null;
		ref = btx.getServiceReference(HerokuServices.class);
		return btx.getService(ref);
	}
	
	public void testGetAPIKey() {
		HerokuServices h = getService();
		
		try {
			h.obtainAPIKey("nouser@noaddres.com", "nopassword"); //$NON-NLS-1$ //$NON-NLS-2$
			fail("The login with nouser@noaddres.com/nopassword has to fail"); //$NON-NLS-1$
		} catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e.getErrorCode());
			assertNotNull(e.getCause());
		}
		
		try {
			h.obtainAPIKey("heroku.junit@bestsolution.at", "nopassword"); //$NON-NLS-1$ //$NON-NLS-2$
			fail("The login has to fail because the password for eclipse-junit@bestsolution.at is different to 'nopassword'"); //$NON-NLS-1$
		} catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e.getErrorCode());
			assertNotNull(e.getCause());
		}
		
		try {
			String apiKey = h.obtainAPIKey("heroku.junit@bestsolution.at", "ooquah2V$"); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull(apiKey);
		} catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}