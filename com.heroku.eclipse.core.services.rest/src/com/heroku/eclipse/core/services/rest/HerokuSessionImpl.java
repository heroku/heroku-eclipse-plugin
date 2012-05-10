package com.heroku.eclipse.core.services.rest;

import java.util.Iterator;
import java.util.List;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Key;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.request.key.KeyAdd;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Class representing a connection ("session") to the Heroku cloud services.   
 * 
 * @author udo.rader@bestsolution.at
 */
public class HerokuSessionImpl implements HerokuSession {
	private final HerokuAPI api;
	private final String apiKey;

	private boolean valid = true;
	
	/**
	 * @param apiKey
	 */
	public HerokuSessionImpl(String apiKey) {
		this.apiKey = apiKey;
		api = new HerokuAPI(apiKey);
	}

	@Override
	public List<App> getAllApps() throws HerokuServiceException {
		if( ! isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}

		List<App> apps = api.listApps();
		return apps;
	}

	@Override
	public void addSSHKey(String sshKey) throws HerokuServiceException {
		if( ! isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}
		try {
			api.addKey(sshKey);
		}
		catch ( RequestFailedException e ) {
			// "key already exists"
			if ( e.getStatusCode() == 422 ) {
				throw new HerokuServiceException(HerokuServiceException.SSH_KEY_ALREADY_EXISTS, e );
			}
			else {
				throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e );
			}
		}
		catch ( Exception e ) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e );
		}
	}

	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		if( ! isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}
		try {
			api.removeKey(sshKey);
		}
		catch ( RequestFailedException e ) {
			// "key not found"
			if ( e.getStatusCode() == 404 ) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, e );
			}
			else {
				throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e );
			}
		}
		catch ( Exception e ) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e );
		}
	}

	/**
	 * @throws HerokuServiceException
	 */
	private void listSSHKeys() throws HerokuServiceException {
		if( ! isValid() ) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}
		try {
			System.err.println("account "+apiKey+" has the following ssh keys:"); //$NON-NLS-1$ //$NON-NLS-2$

			List<Key> keys = api.listKeys();
			Iterator<Key> keyIt = keys.iterator();
			
			while ( keyIt.hasNext() ) {
				Key oneKey = (Key)keyIt.next();
				System.err.println("* email: "+oneKey.getEmail()); //$NON-NLS-1$
				System.err.println("* content: "+oneKey.getContents()); //$NON-NLS-1$
				System.err.println("----"); //$NON-NLS-1$
			}
		}
		catch ( Exception e ) {
			throw new HerokuServiceException(HerokuServiceException.UNKNOWN_ERROR, e );
		}
	}
	public void invalidate() {
		valid = false;
	}
	
	@Override
	public boolean isValid() {
		return valid;
	}
	
	@Override
	public String getAPIKey() {
		return apiKey;
	}
}
