package com.heroku.eclipse.core.services.rest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Key;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;

/**
 * Class representing a connection ("session") to the Heroku cloud services.
 * 
 * @author udo.rader@bestsolution.at
 */
public class RestHerokuSession implements HerokuSession {
	private final HerokuAPI api;
	private final String apiKey;

	private boolean valid = true;

	/**
	 * @param apiKey
	 */
	public RestHerokuSession(String apiKey) {
		this.apiKey = apiKey;
		api = new HerokuAPI(apiKey);
	}

	private void checkValid() throws HerokuServiceException {
		if (!isValid()) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_STATE, "The session is invalid", null); //$NON-NLS-1$
		}
	}

	private String extractErrorField(String msg) {
		Pattern p = Pattern.compile(".*\"error\":\"([^\"]*)\".*"); //$NON-NLS-1$
		Matcher m = p.matcher(msg);
		if (m.matches()) {
			return m.group(1);
		}
		else {
			return msg;
		}
	}

	private HerokuServiceException checkException(RequestFailedException e) {
		switch (e.getStatusCode()) {
			case 403:
				return new HerokuServiceException(HerokuServiceException.NOT_ALLOWED, extractErrorField(e.getResponseBody()), e);
			case 404:
				return new HerokuServiceException(HerokuServiceException.NOT_FOUND, extractErrorField(e.getResponseBody()), e);
			case 406:
				return new HerokuServiceException(HerokuServiceException.NOT_ACCEPTABLE, extractErrorField(e.getResponseBody()), e);
			case 422:
				return new HerokuServiceException(HerokuServiceException.REQUEST_FAILED, extractErrorField(e.getResponseBody()), e);
			default:
				throw e;
		}
	}

	@Override
	public List<App> listApps() throws HerokuServiceException {
		checkValid();
		List<App> apps = api.listApps();
		return apps;
	}

	@Override
	public void addSSHKey(String sshKey) throws HerokuServiceException {
		checkValid();

		try {
			api.addKey(sshKey);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void removeSSHKey(String sshKey) throws HerokuServiceException {
		checkValid();

		try {
			api.removeKey(sshKey);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	/**
	 * 
	 */
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

	@Override
	public List<Key> listSSHKeys() throws HerokuServiceException {
		checkValid();
		return api.listKeys();
	}

	@Override
	public App createApp() throws HerokuServiceException {
		checkValid();
		try {
			return api.createApp();
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void destroyApp(String name) throws HerokuServiceException {
		checkValid();
		try {
			api.destroyApp(name);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public App createApp(App app) throws HerokuServiceException {
		checkValid();
		try {
			return api.createApp(app);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public String renameApp(String currentName, String newName) throws HerokuServiceException {
		checkValid();
		try {
			return api.renameApp(currentName, newName);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}
}
