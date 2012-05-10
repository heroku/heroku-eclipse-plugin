package com.heroku.eclipse.core.services.junit;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.api.App;
import com.heroku.api.Key;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;


public abstract class HerokuSessionTest extends TestCase {
	protected HerokuServices getService() {
		Bundle b = FrameworkUtil.getBundle(HerokuServicesTest.class);
		BundleContext btx = b.getBundleContext();
		
		ServiceReference<HerokuServices> ref = null;
		ref = btx.getServiceReference(HerokuServices.class);
		return btx.getService(ref);
	}
	
	protected HerokuSession getSession() {
		try {
			getService().setAPIKey(Credentials.VALID_JUNIT_APIKEY);
			return getService().getOrCreateHerokuSession();
		}
		catch (Exception e) {
			fail("HerokuService not available: " + e.getMessage());
			return null;
		}
	}
	
	protected HerokuSession getInvalidSession() {
		try {
			getService().setAPIKey("bla");
			return getService().getOrCreateHerokuSession();
		}
		catch (Exception e) {
			fail("HerokuService not available: " + e.getMessage());
			return null;
		}
	}
	
	
}
