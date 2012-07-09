package com.heroku.eclipse.core.services.junit;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.eclipse.core.services.HerokuServices;

/**
 * Base test class for the HerokuServices methods
 * @author udo.rader@bestsolution.at
 */
public class HerokuServicesTest extends TestCase {
	IProgressMonitor pm = null;

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
		s.setAPIKey(getProgressMonitor(), null);
		s.setSSHKey(getProgressMonitor(), null);
	}
	
	protected IProgressMonitor getProgressMonitor() {
		if ( pm == null ) {
			pm = new NullProgressMonitor();
		}
		
		return pm;
	}
}