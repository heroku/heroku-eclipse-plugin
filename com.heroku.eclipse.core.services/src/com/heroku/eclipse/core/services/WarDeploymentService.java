package com.heroku.eclipse.core.services;

import java.io.File;

import com.heroku.api.App;

public interface WarDeploymentService {
	
	void deploy(String apiKey, String appName, File war);
	
}
