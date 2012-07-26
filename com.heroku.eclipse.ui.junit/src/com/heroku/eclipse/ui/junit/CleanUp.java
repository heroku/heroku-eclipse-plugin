package com.heroku.eclipse.ui.junit;

import static org.junit.Assert.fail;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

public class CleanUp extends TestCase {

	private IProgressMonitor pm;

	protected IProgressMonitor getProgressMonitor() {
		if ( pm == null ) {
			pm = new NullProgressMonitor();
		}
		return pm;
	}

	protected HerokuServices getService() {
		Bundle b = FrameworkUtil.getBundle( CleanUp.class );
		BundleContext btx = b.getBundleContext();

		ServiceReference<HerokuServices> ref = null;
		ref = btx.getServiceReference( HerokuServices.class );
		return btx.getService( ref );
	}

	@Test
	public void destroyAllAppsCreatedByUnitTest() throws Exception {
		HerokuServices service = getService();

		try {
			for ( App app : service.listApps( getProgressMonitor() ) ) {
				if ( service.isOwnApp( getProgressMonitor(), app ) ) {
					final String appName = app.getName();
					if ( appName.startsWith( TESTPROJECT_NAME_PREFIX ) ) {
						System.err.println( "app found: " + appName );
						service.destroyApplication( getProgressMonitor(), app );
						System.err.println( "app destroyed: " + appName );
					}
				}
			}
		}
		catch ( HerokuServiceException e ) {
			e.printStackTrace();
			fail( "Could not destry all junit-created apps: " + e.getMessage() );
		}
	}
}
