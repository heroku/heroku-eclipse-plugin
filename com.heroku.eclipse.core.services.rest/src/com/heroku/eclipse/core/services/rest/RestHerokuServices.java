package com.heroku.eclipse.core.services.rest;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.LoginFailedException;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Services class for the Heroclipse plugin, providing access to essential
 * methods of the com.heroku.api.HerokuAPI class
 * 
 * @author udo.rader@bestsolution.at
 */
public class RestHerokuServices implements HerokuServices {
	private HerokuSessionImpl herokuSession;
	private IEclipsePreferences preferences;
	
	private static final String PREF_API_KEY = "apiKey";
	private static final String PREF_SSH_KEY = "sshKey";
	
	private EventAdmin eventAdmin;
	
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}
	
	@Override
	public String obtainAPIKey(String username, String password) throws HerokuServiceException {
		try {
			String apiKey = HerokuAPI.obtainApiKey(username, password);
			return apiKey;
		}
		catch (LoginFailedException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "Unable to log in to account", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e);
		}
		catch (Exception e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to fetch API key", e); //$NON-NLS-1$
			throw new HerokuServiceException(e);
		}
	}

	public HerokuSession getOrCreateHerokuSession() throws HerokuServiceException {
		String apiKey = preferences.get(PREF_API_KEY, null);
		
		if ( apiKey == null ) {
			// invalidate session
			invalidateSession();
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "unable to create session: no API Key configured"); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.NO_API_KEY, "No API Key configured", null); //$NON-NLS-1$
		}
		else if (herokuSession == null) {
			herokuSession = new HerokuSessionImpl( apiKey );
			if( eventAdmin != null ) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_SESSION_INSTANCE, herokuSession);
				
				Event event = new Event(TOPIC_SESSION_CREATED, map);
				eventAdmin.postEvent(event);
			}
		}
		
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
			IEclipsePreferences p = getPreferences();
			
			if( sshKey == null ) {
				p.remove(PREF_SSH_KEY);
				validateSSHKey(sshKey);
				p.put(PREF_SSH_KEY, sshKey);	
			}
			
			p.flush();
		} catch (BackingStoreException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to persist preferences", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}

	@Override
	public void setAPIKey(String apiKey) throws HerokuServiceException {
		try {
			boolean modified = false;
			IEclipsePreferences p = getPreferences();
			if( apiKey == null ) {
				p.remove(PREF_API_KEY);
				modified = true;
			} else {
				if( ! apiKey.equals(getAPIKey()) ) {
					validateAPIKey(apiKey);
					p.put(PREF_API_KEY, apiKey);
					modified = true;
				}
			}
			
			if( modified ) {
				p.flush();
				invalidateSession();	
			}
		} catch (BackingStoreException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to persist preferences", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}
	
	public void validateAPIKey(String apiKey) throws HerokuServiceException {
		try {
			HerokuAPI api = new HerokuAPI(apiKey);
			api.listApps();
		} catch (Throwable e) {
			//TODO We should check for the exception type and HTTP-Error code to find out which problem
			// we have here
			
			Activator.getDefault().getLogger().log(LogService.LOG_WARNING, "validating API key: valdation of key failed", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.INVALID_API_KEY, e);
		}
	}

	@Override
	public void validateSSHKey(String sshKey) throws HerokuServiceException {
		// TODO Auto-generated method stub
		
	}
	
	private void invalidateSession() {
		if( herokuSession != null ) {
			herokuSession.invalidate();
			if( eventAdmin != null ) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_SESSION_INSTANCE, herokuSession);
				
				Event event = new Event(TOPIC_SESSION_INVALID, map);
				eventAdmin.postEvent(event);
			}
		}
		herokuSession = null;
	}
	
	private IEclipsePreferences getPreferences() {
		if( preferences == null ) {
			preferences = InstanceScope.INSTANCE.getNode(Activator.ID);
		}
		return preferences;
	}
}
