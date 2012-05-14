package com.heroku.eclipse.core.services.mockup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.api.HerokuAPI;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Pure mockup, development time service fake for the various heroku API calls
 * 
 * @author udo.rader@bestsolution.at
 */
@SuppressWarnings({"nls","unused"})
public class MockupHerokuServices implements HerokuServices {
	private HerokuSession herokuSession;
	private IEclipsePreferences preferences;
	private ISecurePreferences securePreferences;
	
	private String apiKey;
	
	private static final String PREF_API_KEY = "apiKey"; //$NON-NLS-1$
	private static final String PREF_SSH_KEY = "sshKey"; //$NON-NLS-1$

	private void sleep() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String obtainAPIKey(String username, String password) {
		String apiKey = "Ceterum autem censeo, Carthaginem esse delendam";
		
//		sleep();

		return apiKey;
	}

	@Override
	public HerokuSession getOrCreateHerokuSession() throws HerokuServiceException {
		// invalidate session
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PREF_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR, "secure store unavailable", e); //$NON-NLS-1$
		}

		if ( apiKey == null ) {
			throw new HerokuServiceException(HerokuServiceException.NO_API_KEY, "No API-Key configured", null); //$NON-NLS-1$
		}
		else if (herokuSession == null) {
			herokuSession = new MockupHerokuSession( apiKey );
		}
		
		return herokuSession;
	}

	@Override
	public String getAPIKey() throws HerokuServiceException {
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PREF_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR,e);
		}
		
		return apiKey;
	}

	@Override
	public String getSSHKey() {
		return getPreferences().get(PREF_SSH_KEY, null);
	}
	
	
	public void setSSHKey(String sshKey) throws HerokuServiceException {
		try {
			IEclipsePreferences p = getPreferences();
			if( sshKey == null ) {
				p.remove(PREF_SSH_KEY);
			} else {
				p.put(PREF_SSH_KEY, sshKey);	
			}
			p.flush();
		} catch (BackingStoreException e) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}

	@Override
	public void setAPIKey(String apiKey) throws HerokuServiceException {
		try {
			ISecurePreferences s = getSecurePreferences();
			if( apiKey == null ) {
				s.remove(PREF_API_KEY);
			} else {
				validateAPIKey(apiKey);
				s.put(PREF_API_KEY, apiKey.trim(), true);
			}
			s.flush();
			invalidateSession();
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR,e);
		}
		catch (IOException e) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}
	
	public void validateAPIKey(String apiKey) throws HerokuServiceException {
		
		// all fine for now ...
		
		//throw new HerokuServiceException(HerokuServiceException.INVALID_API_KEY, e);
		
	}

	private void invalidateSession() {
		herokuSession = null;
	}
	
	private IEclipsePreferences getPreferences() {
		if( preferences == null ) {
			preferences = InstanceScope.INSTANCE.getNode("com.heroku.eclipse.core.services.rest");
		}
		return preferences;
	}

	private ISecurePreferences getSecurePreferences() {
		if( securePreferences == null ) {
			ISecurePreferences root = SecurePreferencesFactory.getDefault();
			securePreferences = root.node("com.heroku.eclipse.core.services.rest");
		}
		return securePreferences;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#validateSSHKey(java.lang.String)
	 */
	@Override
	public String[] validateSSHKey(String sshKey) throws HerokuServiceException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#removeSSHKey(java.lang.String)
	 */
	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		// TODO Auto-generated method stub
		
	}
}
