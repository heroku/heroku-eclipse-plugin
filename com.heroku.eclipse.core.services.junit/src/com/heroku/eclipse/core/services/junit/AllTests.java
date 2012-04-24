package com.heroku.eclipse.core.services.junit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static Test suite() {
		TestSuite s = new TestSuite("Core Services Tests");
		s.addTestSuite(HerokuServicesTest.class);
		return s;
	}
}
