package com.heroku.eclipse.useragent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.heroku.api.http.UserAgentValueProvider;

/**
 * UserAgentValueProvider used to identify the Eclipse client to the heroku services. Get repackaged in the com.heroku.core.services.libs bundle.
 * @author tom.schindl@bestsolution.at
 */
public class EclipseUseragent extends UserAgentValueProvider.DEFAULT {
	private final String localUserAgent;
	
	public EclipseUseragent() {
		this.localUserAgent = "heroku-eclipse-plugin/" + getVersion(); //$NON-NLS-1$
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
    
    
    private static String getVersion() {
        Properties projectProperties = new Properties();
        String version = "UNKNOWN";
        
        try {
            InputStream propsStream = EclipseUseragent.class.getClassLoader().getResourceAsStream("heroku-eclipse-useragent.properties");
            if (propsStream != null) {
            	projectProperties.load(propsStream);
            }
            
            String versionOpt = projectProperties.getProperty("com.heroku.eclipse.version");
            if (versionOpt != null) {
            	version = versionOpt;
            }
        } catch (IOException e) {
        	// ignore
        }
        
        return version.replaceFirst("-SNAPSHOT", "");
    }
}