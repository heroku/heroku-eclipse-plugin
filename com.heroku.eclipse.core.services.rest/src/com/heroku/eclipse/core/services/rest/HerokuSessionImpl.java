package com.heroku.eclipse.core.services.rest;

import java.util.List;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Class representing a connection ("session") to the Heroku cloud services.   
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuSessionImpl implements HerokuSession {
	private final HerokuAPI api;
	private final String apiKey;

	private boolean valid = true;
	
	public HerokuSessionImpl(String apiKey) {
		this.apiKey = apiKey;
		api = new HerokuAPI(apiKey);
	}

	@Override
	public List<App> getAllApps() throws HerokuServiceException {
		if( isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}

		List<App> apps = api.listApps();
		return apps;
	}

	@Override
	public void addSSHKey(String sshKey) throws HerokuServiceException {
		if( isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}
		api.addKey(sshKey);
	}

	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		if( isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}
		api.removeKey(sshKey);
	}

	public void invalidate() {
		valid = false;
	}
	
	@Override
	public boolean isValid() {
		return valid;
	}
	
	@Override
	public String getAPIKey() {
		return apiKey;
	}
}
