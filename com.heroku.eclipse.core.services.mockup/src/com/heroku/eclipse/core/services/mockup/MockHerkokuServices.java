package com.heroku.eclipse.core.services.mockup;

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
	private String apiKey;

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
	public String getAPIKey() {
		return apiKey;
	}

	@Override
	public String getSSHKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HerokuSession getOrCreateHerokuSession() {
		// TODO Auto-generated method stub
		return herokuSession;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#setAPIKey(java.lang.String)
	 */
	@Override
	public void setAPIKey(String apiKey) {
		this.apiKey = apiKey;
	}
}
