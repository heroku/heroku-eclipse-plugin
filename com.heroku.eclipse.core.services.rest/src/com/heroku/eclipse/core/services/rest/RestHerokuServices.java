package com.heroku.eclipse.core.services.rest;


import org.osgi.service.log.LogService;

import com.heroku.api.HerokuAPI;
import com.heroku.api.exception.LoginFailedException;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

public class RestHerokuServices implements HerokuServices {

	@Override
	public String doLogin(String username, String password) throws HerokuServiceException {
		try {
			return HerokuAPI.obtainApiKey(username, password);	
		} catch (LoginFailedException e) {
			Activator.getDefault().getLogger().log(LogService.LOG_INFO, "Unable to log in to account", e);
			throw new HerokuServiceException(HerokuServiceException.LOGIN_FAILED_ERROR_CODE, e);
		} catch (Exception e) {
			Activator.getDefault().getLogger().log(LogService.LOG_ERROR, "Unable to fetch API key", e);
			throw new HerokuServiceException(e);
		}
	}

	@Override
	public String[] getAllApps() {
		// TODO Auto-generated method stub
		return null;
	}

}
