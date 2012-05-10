package com.heroku.eclipse.core.services.mockup;

import java.util.ArrayList;
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

	/**
	 * @param apiKey
	 */
	public MockupHerokuSessionImpl(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public List<App> getAllApps() throws HerokuServiceException {
		App oneApp = new App();
		
		oneApp.named("foobar");
		List<App> apps = new ArrayList<App>();
		apps.add(oneApp);
		
		return apps;
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

	@Override
	public boolean isValid() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuSession#getAPIKey()
	 */
	@Override
	public String getAPIKey() {
		// TODO Auto-generated method stub
		return null;
	}

}
