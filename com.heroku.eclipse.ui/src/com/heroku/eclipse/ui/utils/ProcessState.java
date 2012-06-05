package com.heroku.eclipse.ui.utils;

public enum ProcessState {
	IDLE("idle"),
	UP("up"),
	UNKNOWN("__UNKNOWN__");
	
	private String restValue;
	
	private ProcessState(String restValue) {
		this.restValue = restValue;
	}
	
	public static ProcessState parseRest(String restValue) {
		for( ProcessState p : values() ) {
			if( p.restValue.equals(restValue) ) {
				return p;
			}
		}
		return UNKNOWN;
	}
}
