/**
 * 
 */
package com.heroku.eclipse.core.services.model;

/**
 * Simple bean providing a key-value structure 
 * @author udo.rader@bestsolution.at
 */
public class KeyValue {
	private String key;
	private String value;
	
	/**
	 * A surpizing constructor
	 * @param key
	 * @param value
	 */
	public KeyValue( String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
