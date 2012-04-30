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
	final private HerokuAPI api;

	public HerokuSessionImpl(String apiKey) {
		api = new HerokuAPI(apiKey);
	}

	@Override
	public List<App> getAllApps() throws HerokuServiceException {
		List<App> apps = api.listApps();
		return apps;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuSession#addSSHKey(java.lang.String)
	 */
	@Override
	public void addSSHKey(String sshKey) throws HerokuServiceException {
		api.addKey(sshKey);
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuSession#removeSSHKey(java.lang.String)
	 */
	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		api.removeKey(sshKey);
	}
}
