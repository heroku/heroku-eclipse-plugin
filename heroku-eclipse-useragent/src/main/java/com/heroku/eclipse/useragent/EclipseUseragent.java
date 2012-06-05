package com.heroku.eclipse.useragent;

import com.heroku.api.http.UserAgentValueProvider;

public class EclipseUseragent extends UserAgentValueProvider.DEFAULT {
	private final String localUserAgent;
	
	public EclipseUseragent() {
		this.localUserAgent = "heroku-eclipse-plugin v/1.0";
	}
	
	public String getLocalUserAgent() {
        return localUserAgent;
    }

    public String getHeaderValue(String customPart) {
        return localUserAgent + " " + super.getHeaderValue(customPart);
    }
}