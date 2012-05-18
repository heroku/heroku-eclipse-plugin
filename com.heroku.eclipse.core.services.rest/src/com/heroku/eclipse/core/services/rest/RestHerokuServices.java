package com.heroku.eclipse.core.services.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.LoginFailedException;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;

/**
 * Services class for the Heroclipse plugin, providing access to essential
 * methods of the com.heroku.api.HerokuAPI class
 * 
 * @author udo.rader@bestsolution.at
 */
public class RestHerokuServices implements HerokuServices {
	private RestHerokuSession herokuSession;
	private IEclipsePreferences preferences;
	private ISecurePreferences securePreferences;
	
	private static final String PREF_API_KEY = "apiKey"; //$NON-NLS-1$
	private static final String PREF_SSH_KEY = "sshKey"; //$NON-NLS-1$
	
	private EventAdmin eventAdmin;
	
	/**
	 * @param eventAdmin
	 */
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * @param eventAdmin
	 */
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
			throw new HerokuServiceException(HerokuServiceException.LOGIN_FAILED, e);
		}
		catch (Exception e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to fetch API key", e); //$NON-NLS-1$
			throw new HerokuServiceException(e);
		}
	}

	public HerokuSession getOrCreateHerokuSession() throws HerokuServiceException {
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PREF_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR, "unable to access secure store", null); //$NON-NLS-1$
		}
		
		if ( apiKey == null ) {
			throw new HerokuServiceException(HerokuServiceException.NO_API_KEY, "No API-Key configured", null); //$NON-NLS-1$
		}
		else if (herokuSession == null) {
			herokuSession = new RestHerokuSession( apiKey );
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
			if( sshKey == null || sshKey.trim().isEmpty() ) {
				p.remove(PREF_SSH_KEY);
			} else if ( ! sshKey.equals(getSSHKey())) {
				validateSSHKey(sshKey);
				getOrCreateHerokuSession().addSSHKey(sshKey);
				p.put(PREF_SSH_KEY, sshKey);
			}
			p.flush();
		} 
		catch (BackingStoreException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to persist preferences", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}

	@Override
	public void setAPIKey(String apiKey) throws HerokuServiceException {
		try {
			boolean modified = false;
			ISecurePreferences p = getSecurePreferences();
			if( apiKey == null || apiKey.trim().isEmpty() ) {
				p.remove(PREF_API_KEY);
				modified = true;
			} else {
				apiKey = apiKey.trim();
				if( ! apiKey.equals(getAPIKey()) ) {
					validateAPIKey(apiKey);
					p.put(PREF_API_KEY, apiKey, true);
					modified = true;
				}
			}
			
			if( modified ) {
				p.flush();
				invalidateSession();	
			}
		}
		catch (StorageException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to access secure preferences", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR,e);
		}
		catch (IOException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to persist secure preferences", e); //$NON-NLS-1$
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}
	
	public void validateAPIKey(String apiKey) throws HerokuServiceException {
		try {
			HerokuAPI api = new HerokuAPI(apiKey);
			api.listApps();
		} catch (Throwable e) {
			//TODO We should analyze for the exception type and HTTP-Error code to investigate the problem
			throw new HerokuServiceException(HerokuServiceException.INVALID_API_KEY, e);
		}
	}

	@Override
	public String[] validateSSHKey(String sshKey) throws HerokuServiceException {
		String[] parts = null;
		if ( sshKey == null || sshKey.trim().isEmpty() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, "validation of SSH key failed!"); //$NON-NLS-1$
		}
		else {
			parts = sshKey.split(" "); //$NON-NLS-1$
			
			if ( parts.length != 3 ) {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "SSH key '"+sshKey+"' is invalid" ); //$NON-NLS-1$ //$NON-NLS-2$
				throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, "validation of SSH key failed!"); //$NON-NLS-1$
			}
			
			try { 
				DatatypeConverter.parseBase64Binary(parts[1]);
			}	
			catch ( IllegalArgumentException e ) {
				Activator.getDefault().getLogger().log(LogService.LOG_DEBUG, "SSH key '"+sshKey+"' is invalid", e); //$NON-NLS-1$ //$NON-NLS-2$
				throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, "validation of SSH key failed!"); //$NON-NLS-1$
			}
		}
		
		return parts;
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
	
	private ISecurePreferences getSecurePreferences() {
		if( securePreferences == null ) {
			ISecurePreferences root = SecurePreferencesFactory.getDefault();
			securePreferences = root.node(Activator.ID);
		}
		return securePreferences;
	}

	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		String[] keyParts = validateSSHKey(sshKey);
		getOrCreateHerokuSession().removeSSHKey(keyParts[2]);
		setSSHKey(null);
	}

	@Override
	public List<App> listApps() throws HerokuServiceException {
		List<App> apps = new ArrayList<App>();
		apps = getOrCreateHerokuSession().listApps();
		return apps;
	}

	@Override
	public boolean isReady() throws HerokuServiceException {
		boolean isReady = true;
		
		// ensure that we have valid prefs
		String sshKey = null;
		try {
			getOrCreateHerokuSession();
			sshKey = getSSHKey();

			if (sshKey == null || sshKey.trim().isEmpty()) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_PREFERENCES, "Heroku preferences missing or invalid!"); //$NON-NLS-1$
			}
		}
		catch (HerokuServiceException e) {
			// hide "no api key" behind "invalid preferences"
			if (e.getErrorCode() == HerokuServiceException.NO_API_KEY) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_PREFERENCES, "Heroku preferences missing or invalid!", e); //$NON-NLS-1$
			}
			else {
				throw e;
			}
		}

		return isReady;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#listTemplates()
	 */
	@Override
	public List<AppTemplate> listTemplates() throws HerokuServiceException {
		return null;
	}
}
