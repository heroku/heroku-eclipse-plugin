package com.heroku.eclipse.core.services;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;

import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

public interface WarDeploymentService {
	
	void deploy(ProgressMonitor progress, String apiKey, String appName, File war) throws HerokuServiceException;
	
	/*
	 * Deploy-specific wrapper for IProgressMonitor to maintain UI-service separation
	 */
	interface ProgressMonitor {
		IProgressMonitor getIProgressMonitor();
		void start();
		void preparing();
		void uploading();
		void deploying();
		void done();
	}
}
