package com.heroku.eclipse.core.services.mockup;

import java.util.ArrayList;
import java.util.List;

import com.heroku.api.App;
import com.heroku.api.Key;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Class representing a connection ("session") to the Heroku cloud services.   
 * 
 * @author udo.rader@bestsolution.at
 */
@SuppressWarnings({"nls","unused"})
public class MockupHerokuSession implements HerokuSession {
	final private String apiKey;
	private boolean valid = true;

	public MockupHerokuSession(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public List<App> listApps() throws HerokuServiceException {
		ArrayList<App> list = new ArrayList<App>();
		
		return list;
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
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuSession#getAPIKey()
	 */
	@Override
	public String getAPIKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Key> listSSHKeys() throws HerokuServiceException {
		// TODO Auto-generated method stub
		return new ArrayList<Key>();
	}

	@Override
	public App createApp() throws HerokuServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroyApp(String name) throws HerokuServiceException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public App createApp(App app) throws HerokuServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String renameApp(String currentName, String newName)
			throws HerokuServiceException {
		// TODO Auto-generated method stub
		return null;
	}

}
