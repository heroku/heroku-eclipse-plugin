package com.heroku.eclipse.core.services.junit;
import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.junit.common.Credentials;


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
			getService().setAPIKey(new NullProgressMonitor(), Credentials.VALID_JUNIT_APIKEY1);
			return getService().getOrCreateHerokuSession(new NullProgressMonitor());
		}
		catch (Exception e) {
			fail("HerokuService not available: " + e.getMessage());
			return null;
		}
	}
	
	protected HerokuSession getInvalidSession() {
		try {
			getService().setAPIKey(new NullProgressMonitor(), "bla");
			return getService().getOrCreateHerokuSession(new NullProgressMonitor());
		}
		catch (Exception e) {
			fail("HerokuService not available: " + e.getMessage());
			return null;
		}
	}
	
	
}
