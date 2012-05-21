package com.heroku.eclipse.core.services.junit;
import java.util.List;

import com.heroku.api.Key;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;


public class HerokuSessionSshKeyTest extends HerokuSessionTest {

	private void removeAllKeys(HerokuSession session) throws HerokuServiceException {
		for (Key key : session.listSSHKeys()) {
			String desc = key.getContents().split(" ")[2];
			session.removeSSHKey(desc);
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		HerokuSession session = getSession();
		
		// remove all ssh keys
		removeAllKeys(session);
		// add test key 1
		session.addSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1);
	}
	
	@Override
	protected void tearDown() throws Exception {
		HerokuSession session = getSession();
		
		// remove all ssh keys
		removeAllKeys(session);
	}
	
//	public void testAddInvalidKey() {
//		HerokuSession session = getSession();
//		try {
//			session.addSSHKey(Credentials.INVALID_PUBLIC_SSH_KEY1);
//			fail("Invalid SSH Key should not be accepted");
//		}
//		catch (HerokuServiceException e) {
//			assertEquals("expecting request failed", HerokuServiceException.REQUEST_FAILED, e.getErrorCode());
//		}
//	}
	
	public void testAddValidKey() {
		HerokuSession session = getSession();
		try {
			session.addSSHKey(Credentials.VALID_PUBLIC_SSH_KEY2);
		}
		catch (HerokuServiceException e) {
			fail("expected add request to succeed");
		}
	}
	
	public void testRemoveValidKey() {
		HerokuSession session = getSession();
		try {
			session.removeSSHKey(Credentials.VALID_PUBLIC_SSH_KEY1_DESCRIPTION);
		}
		catch (HerokuServiceException e) {
			fail("expected remove request to succeed");
		}
	}
	
//	public void testRemoveInvalidKey() {
//		HerokuSession session = getSession();
//		try {
//			session.removeSSHKey(Credentials.VALID_PUBLIC_SSH_KEY2_DESCRIPTION);
//			fail("expected remove request to fail");
//		}
//		catch (HerokuServiceException e) {
//			assertEquals("expected not found error", HerokuServiceException.NOT_FOUND, e.getErrorCode());
//		}
//	}
	
	public void testListKeys() {
		HerokuSession session = getSession();
		try {
			List<Key> list = session.listSSHKeys();
			assertEquals("amount of registered keys", 1, list.size());
			assertEquals("key", Credentials.VALID_PUBLIC_SSH_KEY1, list.get(0).getContents());
		}
		catch (HerokuServiceException e) {
			e.printStackTrace();
			fail("expected list request to succeed");
		}
	}
	
}
