package com.heroku.eclipse.ui.utils;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

import com.heroku.eclipse.ui.Activator;

/**
 * Static image container  
 * @author tom.schindl@bestsolution.at
 */
public class IconKeys {
	
	public static final String ICON_APPLICATION_OWNER = "/icons/16_16/ApplicationOwner.png";
	public static final String ICON_PROCESS_IDLE = "/icons/16_16/ProcessIdle.png";
	public static final String ICON_PROCESS_UP = "/icons/16_16/ProcessUp.png";
	public static final String ICON_PROCESS_UNKNOWN = "/icons/16_16/ProcessUnknown.png";
	public static final String ICON_APPINFO_EDITOR_ICON = "/icons/16_16/ApplicationInfoEditor.png";
	public static final String ICON_APPSLIST_REFRESH = "/icons/16_16/TangoRefresh.png";
	
	static {
		JFaceResources.getImageRegistry().put(ICON_APPLICATION_OWNER, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_APPLICATION_OWNER));
		JFaceResources.getImageRegistry().put(ICON_PROCESS_IDLE, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_PROCESS_IDLE));
		JFaceResources.getImageRegistry().put(ICON_PROCESS_UP, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_PROCESS_UP));
		JFaceResources.getImageRegistry().put(ICON_PROCESS_UNKNOWN, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_PROCESS_UNKNOWN));
		JFaceResources.getImageRegistry().put(ICON_APPINFO_EDITOR_ICON, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_APPINFO_EDITOR_ICON));
		JFaceResources.getImageRegistry().put(ICON_APPSLIST_REFRESH, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, ICON_APPSLIST_REFRESH));
	}
	
	/**
	 * @param imageKey
	 * @return the wanted Image instance itself
	 */
	public static Image getImage(String imageKey) {
		return JFaceResources.getImage(imageKey);
	}
	
	/**
	 * @param imageKey
	 * @return the wanted image's ImageDescriptor instance
	 */
	public static ImageDescriptor getImageDescriptor(String imageKey) {
		return JFaceResources.getImageRegistry().getDescriptor(imageKey);
	}
}
