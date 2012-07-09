package com.heroku.eclipse.core.services.junit;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.junit.common.Credentials;

public class HerokuServiceSimpleTest extends HerokuServicesTest {

	public void testObtainAPIKey() {
		HerokuServices h = getService();

		try {
			h.obtainAPIKey("nouser@example.com", "nopassword"); //$NON-NLS-1$ //$NON-NLS-2$
			fail("The login with nouser@example.com/nopassword has to fail"); //$NON-NLS-1$
		}
		catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED, e.getErrorCode());
			assertNotNull(e.getCause());
		}

		try {
			h.obtainAPIKey(Credentials.VALID_JUNIT_USER1, "nopassword"); //$NON-NLS-1$ //$NON-NLS-2$
			fail("The login has to fail because the password for eclipse-junit@bestsolution.at is different to 'nopassword'"); //$NON-NLS-1$
		}
		catch (HerokuServiceException e) {
			assertEquals(HerokuServiceException.LOGIN_FAILED, e.getErrorCode());
			assertNotNull(e.getCause());
		}

		try {
			String apiKey = h.obtainAPIKey(Credentials.VALID_JUNIT_USER1, Credentials.VALID_JUNIT_PWD1); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull(apiKey);
			assertEquals(Credentials.VALID_JUNIT_APIKEY1, apiKey);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetAPIKey() {
		HerokuServices h = getService();
		try {
			assertNull(h.getAPIKey());
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			assertEquals(Credentials.VALID_JUNIT_APIKEY1, h.getAPIKey());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testValidateAPIKey() {
		HerokuServices h = getService();
		try {
			h.validateAPIKey(Credentials.VALID_JUNIT_APIKEY1);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			h.validateAPIKey("noapikeyatall");
			fail("The key 'noapikeyatall' is a valid API key");
		}
		catch (HerokuServiceException e) {
			assertEquals("An invalid key error should be thrown", HerokuServiceException.INVALID_API_KEY, e.getErrorCode());
		}
	}

	public void testSetAPIKey() {
		HerokuServices h = getService();
		try {
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			h.setAPIKey("noapikeyatall");
			fail("The key 'noapikeyatall' is a valid API key");
		}
		catch (HerokuServiceException e) {
			assertEquals("Setting should fail with an invalid key command", HerokuServiceException.INVALID_API_KEY, e.getErrorCode());
		}

		try {
			h.setAPIKey("");
			h.setAPIKey(null);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testValidateValidSSHKey() {
		HerokuServices h = getService();
		try {
			String[] parts = h.validateSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
			assertEquals("expecting key validation to return all 3 fragments of the ssh public key", 3, parts.length);
			assertEquals("expecting the description of the validated ssh key to match the real one", Credentials.VALID_PUBLIC_SSH_KEY1_DESCRIPTION, parts[2]);
			
			String[] parts2 = h.validateSSHKey(Credentials.VALID_PUBLIC_SSH_KEY2);
			assertEquals("expecting key validation to return all 3 fragments of the ssh public key", 3, parts2.length);
			assertEquals("expecting the description of the validated ssh key to match the real one", Credentials.VALID_PUBLIC_SSH_KEY2_DESCRIPTION, parts2[2]);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testValidateInvalidSSHKey() {
		HerokuServices h = getService();
		try {
			h.validateSSHKey(Credentials.INVALID_PUBLIC_SSH_KEY1);
			fail("expected invalid ssh key#1 validation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting key validation to fail for ssh keys with only one fragment", HerokuServiceException.INVALID_SSH_KEY, e.getErrorCode());
		}
		
		try {
			h.validateSSHKey(Credentials.INVALID_PUBLIC_SSH_KEY2);
			fail("expected invalid ssh key#2 validation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting key validation to fail for ssh keys with more than 3 fragments", HerokuServiceException.INVALID_SSH_KEY, e.getErrorCode());
		}
		
		try {
			h.validateSSHKey(Credentials.INVALID_PUBLIC_SSH_KEY3);
			fail("expected invalid ssh key#3 validation to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting key validation to fail for ssh keys with invalid public key fragment", HerokuServiceException.INVALID_SSH_KEY, e.getErrorCode());
		}
	}
	
	public void testSetSSHKey() {
		HerokuServices h = getService();
		assertNull(h.getSSHKey());
		try {
			h.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		}
		catch (HerokuServiceException e) {
			assertEquals("SSH key must not be settable w/o API key", HerokuServiceException.NO_API_KEY, e.getErrorCode());
		}

		try {
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			h.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertEquals(Credentials.VALID_PUBLIC_SSH_KEY1, h.getSSHKey());

		try {
			h.setSSHKey("");
			h.setSSHKey(null);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetOrCreateHerokuSession() {
		HerokuServices h = getService();

		try {
			h.getOrCreateHerokuSession();
			fail("There's no API key configured so the tests should fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("Exception should say there's no API key", HerokuServiceException.NO_API_KEY, e.getErrorCode());
		}

		try {
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			HerokuSession session = h.getOrCreateHerokuSession();
			assertNotNull(session);
			assertSame(session, h.getOrCreateHerokuSession());
			assertTrue("The session should be valid", session.isValid());
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			assertTrue("The session should be still valid because the key hasn't changed", session.isValid());

			// applying a different, albeit valid API key
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY2);
			assertFalse("The session should be invalidated because the API-key changed", session.isValid());
			assertNotSame(session, h.getOrCreateHerokuSession());

			// resetting
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			assertFalse("The session should be invalidated because the API-key changed", session.isValid());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testRemoveSSHKey() {
		HerokuServices h = getService();
		assertNull(h.getSSHKey());
		try {
			h.removeSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		}
		catch (HerokuServiceException e) {
			assertEquals("SSH key must not be removeable w/o API key", HerokuServiceException.NO_API_KEY, e.getErrorCode());
		}

		try {
			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			h.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			h.removeSSHKey(Credentials.VALID_PUBLIC_SSH_KEY2);
			fail("expecting removal of unregistered SSH key to fail");
		}
		catch (HerokuServiceException e) {
			assertEquals("expecting removal of unregistered SSH key to throw fitting exception", HerokuServiceException.NOT_FOUND, e.getErrorCode());
		}
		
		try {
			h.removeSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expecting removal of registered SSH key to succeed: "+e.getMessage());
		}
	}


	public void testIsReady() {
		HerokuServices h = getService();
		try {
			h.setAPIKey(null);
			h.setSSHKey(null);

			assertEquals("Service must not signal 'ready' when neither SSH key nor API key is present", false, h.isReady());

			h.setAPIKey(Credentials.VALID_JUNIT_APIKEY1);
			assertEquals("Service must not signal 'ready' when no SSH key is present", false, h.isReady());

			h.setSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
			assertEquals("Service must not signal 'ready' when both API and SSH keys are present", true, h.isReady());

			h.setAPIKey(null);
			assertEquals("Service must not signal 'ready' when no API key is present", false, h.isReady());

			h.setSSHKey(null);

		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}