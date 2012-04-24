package com.heroku.eclipse.core.services.mockup;

import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

public class MockHerkokuServices implements HerokuServices {

	@Override
	public String doLogin(String username, String password) {
		String apiKey = "Ceterum autem censeo, Carthaginem esse delendam";
		
//		sleep();

		return apiKey;
	}

	@Override
	public String[] getAllApps() throws HerokuServiceException {
		String[] appsListing = null;
		
		sleep();
		
		throw new HerokuServiceException();
		
//		return appsListing;
	}

	private void sleep() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
