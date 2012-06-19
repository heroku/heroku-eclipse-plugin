package com.heroku.eclipse.ui;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;

import com.heroku.eclipse.core.services.HerokuServices;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author udo.rader@bestsolution.at
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.heroku.eclipse.ui"; //$NON-NLS-1$

	// The plug-in ID
	public static final String CONSOLE_NAME = "Heroku"; //$NON-NLS-1$
	
	// The shared instance
	private static Activator plugin;
	
	private ServiceTracker<ExtendedLogService, ExtendedLogService> logServiceTracker;
	private ExtendedLogService logService;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		logServiceTracker = new ServiceTracker<ExtendedLogService, ExtendedLogService>(context, ExtendedLogService.class,null);
		logServiceTracker.open();
		
		logService = logServiceTracker.getService();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;

		logServiceTracker.close();
		logServiceTracker = null;

		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public HerokuServices getService() {
		BundleContext btx = this.getBundle().getBundleContext();
		
		ServiceReference<HerokuServices> ref = btx.getServiceReference(HerokuServices.class);
		
		return btx.getService( ref );
	}
	
	public <S> S acquireOSGiService(Class<S> serviceClass) {
		BundleContext btx = this.getBundle().getBundleContext();
		ServiceReference<S> ref = btx.getServiceReference(serviceClass);
		return btx.getService(ref);
	}
	
	public <S> ServiceRegistration<S> registerOSGiService(Class<S> serviceClass, S service, Dictionary<String,String> properties) {
		BundleContext btx = this.getBundle().getBundleContext();
		return btx.registerService(serviceClass, service, properties);
	}
	
	public ServiceRegistration<EventHandler> registerEvenHandler(EventHandler handler, String topic) {
		Dictionary<String,String> properties = new Hashtable<String, String>();
		properties.put(EventConstants.EVENT_TOPIC, topic);
		return registerOSGiService(EventHandler.class, handler, properties);
	}
	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	/**
	 * Returns this bundle's Logger instance 
	 * @return a Logger instance
	 */
	public Logger getLogger() {
		return logService.getLogger(getBundle(), null);
	}
}
