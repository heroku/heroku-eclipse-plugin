package com.heroku.eclipse.core.services.mockup;

import com.heroku.eclipse.core.services.HerokuServices;

public class MockHerkokuServices implements HerokuServices {

	@Override
	public String doLogin(String username, String password) {
		String apiKey = "Ceterum autem censeo, Carthaginem esse delendam";
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		return apiKey;
	}

	@Override
	public String[] getAllApps() {
		// TODO Auto-generated method stub
		return null;
	}
}
