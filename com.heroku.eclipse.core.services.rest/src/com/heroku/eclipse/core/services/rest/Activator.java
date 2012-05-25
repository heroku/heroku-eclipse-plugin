package com.heroku.eclipse.core.services.rest;

import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Entry point for the Heroclipse rest services bundle.    
 * 
 * @author udo.rader@bestsolution.at
 */
public class Activator implements BundleActivator {
	public static final String ID = "com.heroku.eclipse.core.services";
	
	private static BundleContext context;
	private static Activator self;
	private ServiceTracker<ExtendedLogService, ExtendedLogService> logServiceTracker;
	private ExtendedLogService logService;
	
	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		self = this;
		logServiceTracker = new ServiceTracker<ExtendedLogService, ExtendedLogService>(bundleContext, ExtendedLogService.class,null);
		logServiceTracker.open();
		
		logService = logServiceTracker.getService();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		self = null;
		
		logServiceTracker.close();
		logServiceTracker = null;
	}
	
	public static Activator getDefault() {
		return self;
	}

	public Logger getLogger() {
		return logService.getLogger(getContext().getBundle(), null);
	}
	
	public static String getPluginId() {
		return ID;
	}
}