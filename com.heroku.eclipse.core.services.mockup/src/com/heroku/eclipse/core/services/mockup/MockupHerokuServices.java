package com.heroku.eclipse.core.services.mockup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.service.prefs.BackingStoreException;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.eclipse.core.constants.PreferenceConstants;
import com.heroku.eclipse.core.services.HerokuServices;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.AppTemplate;


/**
 * Pure mockup, development time service fake for the various heroku API calls
 * 
 * @author udo.rader@bestsolution.at
 */
@SuppressWarnings({"nls","unused"})
public class MockupHerokuServices implements HerokuServices {
	private HerokuSession herokuSession;
	private IEclipsePreferences preferences;
	private ISecurePreferences securePreferences;
	
	private String apiKey;
	
	private void sleep() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String obtainAPIKey(String username, String password) {
		String apiKey = "Ceterum autem censeo, Carthaginem esse delendam";
		
//		sleep();

		return apiKey;
	}

	@Override
	public HerokuSession getOrCreateHerokuSession() throws HerokuServiceException {
		// invalidate session
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PreferenceConstants.P_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR, "secure store unavailable", e); //$NON-NLS-1$
		}

		if ( apiKey == null ) {
			throw new HerokuServiceException(HerokuServiceException.NO_API_KEY, "No API-Key configured", null); //$NON-NLS-1$
		}
		else if (herokuSession == null) {
			herokuSession = new MockupHerokuSession( apiKey );
		}
		
		return herokuSession;
	}

	@Override
	public String getAPIKey() throws HerokuServiceException {
		String apiKey = null;
		try {
			apiKey = getSecurePreferences().get(PreferenceConstants.P_API_KEY, null);
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR,e);
		}
		
		return apiKey;
	}

	@Override
	public String getSSHKey() {
		return getPreferences().get(PreferenceConstants.P_SSH_KEY, null);
	}
	
	
	public void setSSHKey(String sshKey) throws HerokuServiceException {
		try {
			IEclipsePreferences p = getPreferences();
			if( sshKey == null ) {
				p.remove(PreferenceConstants.P_SSH_KEY);
			} else {
				p.put(PreferenceConstants.P_SSH_KEY, sshKey);	
			}
			p.flush();
		} catch (BackingStoreException e) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}

	@Override
	public void setAPIKey(String apiKey) throws HerokuServiceException {
		try {
			ISecurePreferences s = getSecurePreferences();
			if( apiKey == null ) {
				s.remove(PreferenceConstants.P_API_KEY);
			} else {
				validateAPIKey(apiKey);
				s.put(PreferenceConstants.P_API_KEY, apiKey.trim(), true);
			}
			s.flush();
			invalidateSession();
		}
		catch (StorageException e) {
			throw new HerokuServiceException(HerokuServiceException.SECURE_STORE_ERROR,e);
		}
		catch (IOException e) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR,e);
		}
	}
	
	public void validateAPIKey(String apiKey) throws HerokuServiceException {
		
		// all fine for now ...
		
		//throw new HerokuServiceException(HerokuServiceException.INVALID_API_KEY, e);
		
	}

	private void invalidateSession() {
		herokuSession = null;
	}
	
	private IEclipsePreferences getPreferences() {
		if( preferences == null ) {
			preferences = InstanceScope.INSTANCE.getNode("com.heroku.eclipse.core.services.rest");
		}
		return preferences;
	}

	private ISecurePreferences getSecurePreferences() {
		if( securePreferences == null ) {
			ISecurePreferences root = SecurePreferencesFactory.getDefault();
			securePreferences = root.node("com.heroku.eclipse.core.services.rest");
		}
		return securePreferences;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#validateSSHKey(java.lang.String)
	 */
	@Override
	public String[] validateSSHKey(String sshKey) throws HerokuServiceException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#removeSSHKey(java.lang.String)
	 */
	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#listApps()
	 */
	@Override
	public List<App> listApps() throws HerokuServiceException {
		List<App> dummy = new ArrayList<App>();
		return dummy;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#isReady()
	 */
	@Override
	public boolean isReady() throws HerokuServiceException {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.heroku.eclipse.core.services.HerokuServices#listTemplates()
	 */
	@Override
	public List<AppTemplate> listTemplates() throws HerokuServiceException {
		List<AppTemplate> templates = new ArrayList<AppTemplate>();
		
		AppTemplate a = new AppTemplate();
		a.setDisplayName("Web app with Spring and Tomcat");
		a.setTemplateName("template-java-spring-hibernate");
		a.setLanguage("Java");
		a.setId(1);
		
		AppTemplate b = new AppTemplate();
		b.setDisplayName("Containerless web app with Embedded Jetty");
		b.setTemplateName("template-java-embedded-jetty");
		b.setLanguage("Java");
		b.setId(2);
		
		AppTemplate c = new AppTemplate();
		c.setDisplayName("Web app with Play! Framework");
		c.setTemplateName("tempalte-java-play");
		c.setLanguage("Java");
		c.setId(3);
		
		AppTemplate d = new AppTemplate();
		d.setDisplayName("RESTful API with JAX-RS");
		d.setTemplateName("template-java-jaxrs");
		d.setLanguage("Java");
		d.setId(4);
		
		templates.add( a );
		templates.add( b );
		templates.add( c );
		templates.add( d );
		
		return templates;
	}
}
