package com.heroku.eclipse.core.services.mockup;

import java.util.List;

import com.heroku.api.App;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Class representing a connection ("session") to the Heroku cloud services.   
 * 
 * @author udo.rader@bestsolution.at
 */
@SuppressWarnings({"nls","unused"})
public class MockupHerokuSessionImpl implements HerokuSession {
	final private String apiKey;

	public MockupHerokuSessionImpl(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public List<App> getAllApps() throws HerokuServiceException {
		return null;
	}

	@Override
	public void addSSHKey(String sshKey) throws HerokuServiceException {
		// nada
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuSession#removeSSHKey(java.lang.String)
	 */
	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		// TODO Auto-generated method stub
	}

}
