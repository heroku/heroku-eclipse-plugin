package com.heroku.eclipse.core.services;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

import com.heroku.api.App;

public interface WarDeploymentService {
	
	void deploy(IProgressMonitor pm, String apiKey, String appName, File war);
	
}
