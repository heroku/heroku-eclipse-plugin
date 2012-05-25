package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

import com.heroku.eclipse.ui.Activator;

public class IconKeys {
	public static final String ICON_APPLICATION_OWNER = "/icons/16_16/ApplicationOwner.png";
	
	static {
		JFaceResources.getImageRegistry().put(ICON_APPLICATION_OWNER, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_APPLICATION_OWNER));
	}
	
	public static Image getImage(String imageKey) {
		return JFaceResources.getImage(ICON_APPLICATION_OWNER);
	}
}
