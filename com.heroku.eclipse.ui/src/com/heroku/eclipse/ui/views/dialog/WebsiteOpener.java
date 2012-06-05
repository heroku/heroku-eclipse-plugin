package com.heroku.eclipse.ui.views.dialog;

import com.heroku.api.App;

/**
 * Simple interface providing everything we need to open a Heroku App using a webbrowser (Eclipse internal or external)
 * @author tom.schindl@bestsolution.at
 */
public interface WebsiteOpener {
	/**
	 * Opens the App in Eclipse's internal browser
	 * @param application
	 */
	public void openInternal(App application);
}
