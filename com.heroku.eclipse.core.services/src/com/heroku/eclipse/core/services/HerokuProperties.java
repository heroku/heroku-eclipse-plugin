package com.heroku.eclipse.core.services;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author udo.rader@bestsolution.at
 *
 */
public class HerokuProperties {
	private static final String			BUNDLE_NAME			= "com.heroku.eclipse.core.services.heroku";	//$NON-NLS-1$
	private static final ResourceBundle	RESOURCE_BUNDLE		= ResourceBundle.getBundle( BUNDLE_NAME );
	
	private HerokuProperties() {}

	/**
	 * Returns the unformatted value associated to the given key.
	 * 
	 * @param key
	 * @return the unformatted value
	 */
	public static String getString( String key ) {
		try {
			return RESOURCE_BUNDLE.getString( key );
		}
		catch ( MissingResourceException e ) {
			return '!' + key + '!';
		}
	}
}
