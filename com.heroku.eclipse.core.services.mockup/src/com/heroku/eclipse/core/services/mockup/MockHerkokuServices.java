package com.heroku.eclipse.core.services.mockup;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
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
public class MockHerkokuServices implements HerokuServices {
	private HerokuSession herokuSession;
	private IEclipsePreferences preferences;
	
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
	public HerokuSession getOrCreateHerokuSession() {
		// TODO Auto-generated method stub
		return herokuSession;
	}

	@Override
	public String getAPIKey() {
		return getPreferences().get(PREF_API_KEY, null);
	}

	@Override
	public String getSSHKey() {
		return getPreferences().get(PREF_SSH_KEY, null);
	}
	
	
	public void setSSHKey(String sshKey) throws HerokuServiceException {
		try {
			//TODO Should we validate the SSH-Key???
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
			
			IEclipsePreferences p = getPreferences();
			if( apiKey == null ) {
				p.remove(PREF_API_KEY);
			} else {
				validateAPIKey(apiKey);
				p.put(PREF_API_KEY, apiKey.trim());
			}
			p.flush();
			invalidateSession();
		} catch (BackingStoreException e) {
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
}
