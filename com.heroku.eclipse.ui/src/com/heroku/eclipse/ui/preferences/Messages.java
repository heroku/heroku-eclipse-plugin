package com.heroku.eclipse.ui.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.heroku.eclipse.ui.preferences.messages"; //$NON-NLS-1$
	public static String HerokuPreferencePage_Title;
	public static String HerokuPreferencePage_Email;
	public static String HerokuPreferencePage_Password;
	public static String HerokuPreferencePage_APIKey;
	public static String HerokuPreferencePage_SSHKey;
	public static String HerokuPreferencePage_GetAPIKey;
	public static String HerokuPreferencePage_Validate;
	public static String HerokuPreferencePage_Generate;
	public static String HerokuPreferencePage_Update;
	public static String HerokuPreferencePage_Clear;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
