package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

import com.heroku.eclipse.ui.Activator;

public class IconKeys {
	public static final String ICON_APPLICATION_OWNER = "/icons/16_16/ApplicationOwner.png";
	public static final String ICON_PROCESS_IDLE = "/icons/16_16/ProcessIdle.png";
	public static final String ICON_PROCESS_UP = "/icons/16_16/ProcessUp.png";
	public static final String ICON_PROCESS_UNKNOWN = "/icons/16_16/ProcessUnknown.png";
	
	
	static {
		JFaceResources.getImageRegistry().put(ICON_APPLICATION_OWNER, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_APPLICATION_OWNER));
		JFaceResources.getImageRegistry().put(ICON_PROCESS_IDLE, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_PROCESS_IDLE));
		JFaceResources.getImageRegistry().put(ICON_PROCESS_UP, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_PROCESS_UP));
		JFaceResources.getImageRegistry().put(ICON_PROCESS_UNKNOWN, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_PROCESS_UNKNOWN));
	}
	
	public static Image getImage(String imageKey) {
		return JFaceResources.getImage(imageKey);
	}
}
