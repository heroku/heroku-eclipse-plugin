package com.heroku.eclipse.ui;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;

/**
 * Class dealing how we retrieve I18n messages for the Heroclipse plugin   
 * 
 * @author udo.rader@bestsolution.at
 */
public class Messages extends NLS {
	
	private static final String BUNDLE_NAME = "i18n.messages"; //$NON-NLS-1$
	
	private static final ResourceBundle	RESOURCE_BUNDLE	= ResourceBundle.getBundle( BUNDLE_NAME );

	private Messages() {
	}

	/**
	 * Returns the unformatted value associated to the given key.
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
	
	/**
	 * Returns the formatted value associated to the given key.
	 * @param key
	 * @param replacements the replacements objects
	 * @return the formatted value
	 */
	public static String getFormattedString( String key, Object ... replacements ) {
		try {
			return MessageFormat.format( RESOURCE_BUNDLE.getString( key ), replacements );
		}
		catch ( MissingResourceException e ) {
			return '!' + key + '!';
		}
	}
}
