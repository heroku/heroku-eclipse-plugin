package com.heroku.eclipse.core.services.rest;

import java.util.List;

import org.osgi.service.log.LogService;

import com.heroku.api.App;
import com.heroku.api.Heroku;
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
	private HerokuSession herokuSession;
	private String apiKey;

	@Override
	public String obtainAPIKey(String username, String password) throws HerokuServiceException {
		try {
			apiKey = HerokuAPI.obtainApiKey(username, password);
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
		// invalidate session
		if ( apiKey == null ) {
			herokuSession = null;
		}
		else if (herokuSession == null) {
			herokuSession = new HerokuSessionImpl( apiKey );
		}
		
		return herokuSession;
	}

	@Override
	public String getAPIKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSSHKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#setAPIKey(java.lang.String)
	 */
	@Override
	public void setAPIKey(String apiKey) {
		// TODO Auto-generated method stub
		
	}

}
