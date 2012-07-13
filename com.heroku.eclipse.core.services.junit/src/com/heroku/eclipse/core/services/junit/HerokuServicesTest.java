package com.heroku.eclipse.core.services.junit;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;

/**
 * Base test class for the HerokuServices methods
 * @author udo.rader@bestsolution.at
 */
public class HerokuServicesTest extends TestCase {
	IProgressMonitor pm = null;
	List<AppTemplate> templatesList = null;

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
	
	protected AppTemplate getTestTemplate() throws HerokuServiceException {
		if ( templatesList == null ) {
			templatesList = getService().listTemplates(getProgressMonitor());
			if ( templatesList.size() <= 0 ) {
				throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, "unable to list templates");
			}
		}
		
		return templatesList.get(0);
	}

}