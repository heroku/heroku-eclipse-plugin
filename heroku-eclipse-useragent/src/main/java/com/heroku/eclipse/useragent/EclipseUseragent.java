package com.heroku.eclipse.useragent;

import com.heroku.api.http.UserAgentValueProvider;

/**
 * UserAgentValueProvider used to identify the Eclipse client to the heroku services. Get repackaged in the com.heroku.api bundle.
 * @author tom.schindl@bestsolution.at
 */
public class EclipseUseragent extends UserAgentValueProvider.DEFAULT {
	private final String localUserAgent;
	
	/**
	 * 
	 */
	public EclipseUseragent() {
		this.localUserAgent = "heroku-eclipse-plugin/1.0.1"; //$NON-NLS-1$
	}
	
	/**
	 * @return the user agent string
	 */
	public String getLocalUserAgent() {
        return localUserAgent;
    }

    public String getHeaderValue(String customPart) {
        return localUserAgent + " " + super.getHeaderValue(customPart); //$NON-NLS-1$
    }
}