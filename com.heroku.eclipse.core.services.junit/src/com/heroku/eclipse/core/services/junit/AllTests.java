package com.heroku.eclipse.core.services.junit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static Test suite() {
		TestSuite s = new TestSuite("Core Services Tests"); //$NON-NLS-1$
		s.addTestSuite(HerokuServicesTest.class);
		
		s.addTestSuite(HerokuSessionSimpleTest.class);
		s.addTestSuite(HerokuSessionSshKeyTest.class);
		s.addTestSuite(HerokuSessionAppTest.class);
		return s;
	}
}
