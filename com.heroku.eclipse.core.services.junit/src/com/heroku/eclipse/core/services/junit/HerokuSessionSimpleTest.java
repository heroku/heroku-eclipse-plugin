package com.heroku.eclipse.core.services.junit;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;


public class HerokuSessionSimpleTest extends HerokuSessionTest {

	public void testGetAPIKey() {
		HerokuSession session = getSession();
		String apiKey = session.getAPIKey();
		assertEquals("The returned api key must be the same as the provided api key.", Credentials.VALID_JUNIT_APIKEY, apiKey);
	}
	
	public void testIsValid() {
		HerokuSession session = getSession();
		assertEquals("The session is expected to be valid", true, session.isValid());
	}
	
	public void testIsNotValid() throws HerokuServiceException {
		getService().setAPIKey("bla");
		HerokuSession session = getSession();
		
		assertEquals("The session is expected to be invalid", false, session.isValid());
	}
	
}
