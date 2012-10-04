package com.heroku.eclipse.core.services.rest;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.heroku.api.App;
import com.heroku.api.Collaborator;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Key;
import com.heroku.api.Proc;
import com.heroku.api.User;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.request.log.Log.LogRequestBuilder;
import com.heroku.api.request.log.LogStreamResponse;
import com.heroku.eclipse.core.services.HerokuSession;
import com.heroku.eclipse.core.services.exceptions.HerokuServiceException;
import com.heroku.eclipse.core.services.model.HerokuDyno;
import com.heroku.eclipse.core.services.model.HerokuProc;

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
	 * Pattern helpful for determining the real meaning of a HTTP 422 response code, mapped to {@link HerokuServiceException#NOT_ACCEPTABLE}
	 */
	private static Pattern PATTERN_NOT_ACCEPTABLE = Pattern.compile(".*(Name is already taken|must start with a letter and can only contain|Please verify your account in order to change resources).*"); //$NON-NLS-1$
	
	/**
	 * Pattern helpful for determining the real meaning of a HTTP 422 response code, mapped to {@link HerokuServiceException#NOT_FOUND}
	 */
	private static Pattern PATTERN_NOT_FOUND = Pattern.compile(".*(No such type as).*"); //$NON-NLS-1$
	
	/**
	 * Pattern helpful for determining the real meaning of a HTTP 422 response code, mapped to {@link HerokuServiceException#NOT_ALLOWED}
	 */
	private static Pattern PATTERN_NOT_ALLOWED = Pattern.compile(".*(The owner of.*must be verified before you can scale processes|only the owner|Only the app owner).*"); //$NON-NLS-1$
	
	/**
	 * Pattern for checking if an account is unverified
	 */
	private static Pattern PATTERN_VERIFY_ACCOUNT = Pattern.compile(".*(Please verify your account to install this add-on).*");
	
	/**
	 * @param apiKey
	 */
	public RestHerokuSession(String apiKey) {
		this.apiKey = apiKey;
		api = new HerokuAPI(apiKey);
	}

	private void checkValid() throws HerokuServiceException {
		if (!isValid()) {
			throw new HerokuServiceException(HerokuServiceException.INVALID_SESSION, "The session is invalid", null); //$NON-NLS-1$
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
				// the majority of "interesting" responses from the Heroku API
				// all share HTTP code 422. So unfortunately we have to use the
				// response body to separate between fiction and fact.
				if (PATTERN_NOT_ACCEPTABLE.matcher(e.getResponseBody()).matches()) {
					return new HerokuServiceException(HerokuServiceException.NOT_ACCEPTABLE, extractErrorField(e.getResponseBody()), e);
				}
				else if (PATTERN_NOT_FOUND.matcher(e.getResponseBody()).matches()) {
					return new HerokuServiceException(HerokuServiceException.NOT_FOUND, extractErrorField(e.getResponseBody()), e);
				}
				else if (PATTERN_NOT_ALLOWED.matcher(e.getResponseBody()).matches()) {
					return new HerokuServiceException(HerokuServiceException.NOT_ALLOWED, extractErrorField(e.getResponseBody()), e);
				}
				else if (PATTERN_VERIFY_ACCOUNT.matcher(e.getResponseBody()).matches()) {
					return new HerokuServiceException(HerokuServiceException.UNVERIFIED, extractErrorField(e.getResponseBody()), e);
				}
				else {
					return new HerokuServiceException(HerokuServiceException.REQUEST_FAILED, extractErrorField(e.getResponseBody()), e);
				}
			default:
				throw e;
		}
	}

	@Override
	public List<App> listApps() throws HerokuServiceException {
		checkValid();
		List<App> list = api.listApps();
		return list;
	}

	@Override
	public void addSSHKey(String sshKey) throws HerokuServiceException {
		checkValid();

		try {
			api.addKey(sshKey);
		}
		catch (RequestFailedException e) {
			if (e.getStatusCode() == 422) {
				throw new HerokuServiceException(HerokuServiceException.INVALID_SSH_KEY, extractErrorField(e.getResponseBody()), e);
			}
			else {
				throw checkException(e);
			}
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
	public void destroyApp(App app) throws HerokuServiceException {
		destroyApp(app.getName());
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

	@Override
	public App cloneTemplate(String templateName) throws HerokuServiceException {
		checkValid();
		try {
			return api.cloneApp(templateName);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public App getApp(String appName) throws HerokuServiceException {
		checkValid();
		try {
			return api.getApp(appName);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public User getUserInfo() throws HerokuServiceException {
		checkValid();
		try {
			return api.getUserInfo();
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	public void restart(App app) throws HerokuServiceException {
		checkValid();
		try {
			api.restart(app.getName());
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public List<Collaborator> getCollaborators(App app) throws HerokuServiceException {
		checkValid();
		try {
			return api.listCollaborators(app.getName());
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	public void addCollaborator(App app, String email) throws HerokuServiceException {
		checkValid();
		try {
			api.addCollaborator(app.getName(), email);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	public void removeCollaborator(App app, String email) throws HerokuServiceException {
		checkValid();
		try {
			api.removeCollaborator(app.getName(), email);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void transferApplication(App app, String newOwner) throws HerokuServiceException {
		checkValid();
		try {
			api.transferApp(app.getName(), newOwner);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	public List<Proc> listProcesses(App app) throws HerokuServiceException {
		checkValid();
		try {
			return api.listProcesses(app.getName());
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public InputStream getApplicationLogStream(String appName) throws HerokuServiceException {
		checkValid();
		try {
			LogStreamResponse stream = api.getLogs(new LogRequestBuilder().app(appName).tail(true));
			return stream.openStream();
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public InputStream getProcessLogStream(String appName, String processName) throws HerokuServiceException {
		checkValid();
		try {
			LogStreamResponse stream = api.getLogs(new LogRequestBuilder().app(appName).ps(processName).tail(true));
			return stream.openStream();
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void restart(Proc proc) throws HerokuServiceException {
		checkValid();
		try {
			api.restartProcessByName(proc.getAppName(), proc.getProcess());
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void restartDyno(HerokuDyno dyno) throws HerokuServiceException {
		checkValid();
		try {
			api.restartProcessByType(dyno.getAppName(), dyno.getName());
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public boolean appNameExists(String appName) throws HerokuServiceException {
		checkValid();
		try {
			return api.appExists(appName);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public App createAppFromTemplate(App app, String templateName) throws HerokuServiceException {
		checkValid();
		try {
			if ( app == null ) {
				return api.cloneApp(templateName);
			}
			else {
				return api.cloneApp(templateName, app);
			}
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void addEnvVariables(String appName, Map<String, String> envMap) throws HerokuServiceException {
		checkValid();
		try {
			api.addConfig(appName, envMap);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public Map<String, String> listEnvVariables(String appName) throws HerokuServiceException {
		checkValid();
		try {
			return api.listConfig(appName);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void removeEnvVariable(String appName, String envKey) throws HerokuServiceException {
		checkValid();
		try {
			api.removeConfig(appName, envKey);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}

	@Override
	public void scaleProcess(String appName, String processType, int quantity) throws HerokuServiceException {
		checkValid();
		try {
			api.scaleProcess(appName, processType, quantity);
		}
		catch (RequestFailedException e) {
			throw checkException(e);
		}
	}
}
