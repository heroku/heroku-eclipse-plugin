package com.heroku.eclipse.core.services.junit;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.eclipse.core.services.HerokuServices;

public class HerokuServicesTest extends TestCase {

	protected HerokuServices getService() {
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
}